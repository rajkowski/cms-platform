/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zeroio.platform.presentation.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchRepository;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.controller.PageRequest;
import com.simisinc.platform.presentation.controller.UserSession;
import com.sun.net.httpserver.HttpServer;
import com.zeroio.platform.domain.model.cms.SearchCriteria;

import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

class AskAIJsonServiceTest {

  @Test
  void sendsQuestionAndSourceToOllama() throws Exception {
    AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    ObjectMapper mapper = new ObjectMapper();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/chat", exchange -> {
      requestBody.set(mapper.readTree(exchange.getRequestBody()));
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      byte[] body = "{\"message\":{\"content\":\"A grounded answer\"}}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      SearchResult result = new SearchResult();
      result.setPageTitle("Policy");
      String fullContent = "The answer is in this source. ".repeat(200) + "FULL_RESULT_END";
      result.setSourceText(fullContent);
      String answer = AskAIJsonService.generate("http://127.0.0.1:" + server.getAddress().getPort(), "mistral:7b", "secret",
          "What is the policy?", List.of(result), "Unrelated current page.", false);
      assertEquals("A grounded answer", answer);
      assertEquals("Bearer secret", authorization.get());
      assertEquals("mistral:7b", requestBody.get().path("model").asText());
      assertFalse(requestBody.get().path("stream").asBoolean());
      String prompt = requestBody.get().path("messages").get(1).path("content").asText();
      assertTrue(prompt.contains(fullContent));
      assertTrue(prompt.contains("What is the policy?"));
      assertTrue(prompt.indexOf("Full content:") < prompt.indexOf("Current page (supplementary context):"));
      assertTrue(requestBody.get().path("messages").get(0).path("content").asText().contains("do not mention that"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void rejectsUnsuccessfulOllamaResponses() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/chat", exchange -> {
      exchange.sendResponseHeaders(503, -1);
      exchange.close();
    });
    server.start();
    try {
      assertThrows(IllegalStateException.class, () -> AskAIJsonService.generate("http://127.0.0.1:" + server.getAddress().getPort(),
          "mistral:7b", null, "Question", List.of()));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void fallbackSearchUsesDistinctQuestionKeywords() {
    assertEquals("leave OR policy", AskAIJsonService.keywordQuery("What is the leave policy and leave?"));
  }

  @Test
  void prioritizesWebPagesOverAttachments() {
    SearchResult attachment = new SearchResult();
    attachment.setType(SearchCriteria.ATTACHMENTS);
    SearchResult page = new SearchResult();
    page.setType(SearchCriteria.PAGES);
    SearchResult post = new SearchResult();
    post.setType(SearchCriteria.POSTS);
    assertEquals(List.of(page, post, attachment), AskAIJsonService.prioritizeResults(List.of(attachment, page, post)));
  }

  @Test
  void reranksByBestPassageScoreAndKeepsTiesStable() {
    ScoringModel model = mock(ScoringModel.class);
    SearchResult page = new SearchResult();
    page.setType(SearchCriteria.PAGES);
    page.setSourceText("A".repeat(1200) + "relevant passage");
    SearchResult attachment = new SearchResult();
    attachment.setType(SearchCriteria.ATTACHMENTS);
    attachment.setSourceText("relevant attachment");
    SearchResult blank = new SearchResult();
    when(model.score(any(String.class), org.mockito.ArgumentMatchers.eq("question"))).thenAnswer(invocation -> {
      String passage = invocation.getArgument(0);
      return Response.from(passage.contains("relevant") ? 0.9 : 0.1);
    });
    assertEquals(List.of(page, attachment, blank),
        AskAIJsonService.rerankResults("question", List.of(page, attachment, blank), model));
    verify(model).score("relevant attachment", "question");
  }

  @Test
  void rerankingSelectsHighestScoresBeforeApplyingLimit() {
    ScoringModel model = mock(ScoringModel.class);
    List<SearchResult> results = new java.util.ArrayList<>();
    for (int index = 0; index < 8; index++) {
      SearchResult result = new SearchResult();
      result.setSourceText(Integer.toString(index));
      results.add(result);
    }
    when(model.score(any(String.class), org.mockito.ArgumentMatchers.eq("question")))
        .thenAnswer(invocation -> Response.from(Double.parseDouble(invocation.getArgument(0))));
    assertEquals(List.of(results.get(7), results.get(6), results.get(5), results.get(4), results.get(3), results.get(2)),
        AskAIJsonService.rerankResults("question", results, model));
  }

  @Test
  void rerankingFallsBackWhenScoringFailsOrReturnsInvalidScores() {
    ScoringModel model = mock(ScoringModel.class);
    SearchResult attachment = new SearchResult();
    attachment.setType(SearchCriteria.ATTACHMENTS);
    attachment.setSourceText("attachment");
    SearchResult page = new SearchResult();
    page.setType(SearchCriteria.PAGES);
    page.setSourceText("page");
    when(model.score(any(String.class), org.mockito.ArgumentMatchers.eq("question")))
        .thenThrow(new IllegalStateException("model failed"))
        .thenReturn(Response.from(Double.NaN));
    List<SearchResult> results = List.of(attachment, page);
    assertEquals(List.of(page, attachment), AskAIJsonService.rerankResults("question", results, model));
    assertEquals(List.of(page, attachment), AskAIJsonService.rerankResults("question", results, model));
  }

  @Test
  void rejectsUnsearchableAndCrossOriginRefererPages() {
    PageRequest request = mock(PageRequest.class);
    when(request.getHeader("Referer")).thenReturn("https://example.com/policy");
    when(request.getScheme()).thenReturn("https");
    when(request.getServerName()).thenReturn("example.com");
    when(request.getPort()).thenReturn(443);
    when(request.getContextPath()).thenReturn("");
    WebPage currentPage = new WebPage();
    currentPage.setSearchable(false);
    try (var pages = mockStatic(LoadWebPageCommand.class)) {
      pages.when(() -> LoadWebPageCommand.loadByLink("/policy")).thenReturn(currentPage);
      assertFalse(AskAIJsonService.isSearchableRefererPage(request));
      when(request.getHeader("Referer")).thenReturn("https://elsewhere.example/policy");
      assertFalse(AskAIJsonService.isSearchableRefererPage(request));
    }
  }

  @Test
  void searchSpecificationUsesTheCurrentUserAndRoles() {
    JsonServiceContext context = mock(JsonServiceContext.class);
    UserSession session = mock(UserSession.class);
    when(context.getUserSession()).thenReturn(session);
    when(session.getUserId()).thenReturn(42L);
    when(context.hasRole("content-manager")).thenReturn(true);
    try (var properties = mockStatic(LoadSitePropertyCommand.class)) {
      properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("site.regions.enabled")).thenReturn(false);
      UnifiedSearchSpecification specification = SearchJsonService.createSpecification(context, "policy");
      assertEquals(42L, specification.getForUserId());
      assertEquals("policy", specification.getSearchTerm());
      assertTrue(specification.isContentManager());
      assertFalse(specification.isDataManager());
    }
  }

  @Test
  void noSearchResultsReturnsAnAnswerWithoutCallingOllama() {
    AskAIJsonService service = spy(new AskAIJsonService());
    doReturn("http://127.0.0.1:1").when(service).getenv("OLLAMA_URL");
    JsonServiceContext context = mock(JsonServiceContext.class);
    PageRequest request = mock(PageRequest.class);
    UserSession session = mock(UserSession.class);
    when(context.getRequest()).thenReturn(request);
    when(context.getUserSession()).thenReturn(session);
    when(session.getUserId()).thenReturn(42L);
    when(request.getParameter("question")).thenReturn("What is the leave policy?");
    try (var properties = mockStatic(LoadSitePropertyCommand.class);
        var repository = mockStatic(UnifiedSearchRepository.class)) {
      properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("ai.enabled")).thenReturn(true);
      properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("ai.askai.enabled")).thenReturn(true);
      repository.when(() -> UnifiedSearchRepository.findAll(any(), any())).thenReturn(List.of());
      service.post(context);
      repository.verify(() -> UnifiedSearchRepository.findAll(any(), any()), org.mockito.Mockito.times(2));
      verify(context).writeOk(contains("couldn't find relevant content"));
    }
  }

  @Test
  void returnsAnAnswerAndSourcesForVisibleSearchResults() throws Exception {
    AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    ObjectMapper mapper = new ObjectMapper();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/chat", exchange -> {
      requestBody.set(mapper.readTree(exchange.getRequestBody()));
      byte[] body = "{\"message\":{\"content\":\"A grounded answer\"}}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      AskAIJsonService service = spy(new AskAIJsonService());
      doReturn("http://127.0.0.1:" + server.getAddress().getPort()).when(service).getenv("OLLAMA_URL");
      doReturn(null).when(service).getenv("OLLAMA_MODEL");
      doReturn(null).when(service).getenv("OLLAMA_API_KEY");
      JsonServiceContext context = mock(JsonServiceContext.class);
      PageRequest request = mock(PageRequest.class);
      UserSession session = mock(UserSession.class);
      when(context.getRequest()).thenReturn(request);
      when(request.getHeader("Referer")).thenReturn("https://example.com/policy");
      when(request.getScheme()).thenReturn("https");
      when(request.getServerName()).thenReturn("example.com");
      when(request.getPort()).thenReturn(443);
      when(request.getContextPath()).thenReturn("");
      when(context.getUserSession()).thenReturn(session);
      when(session.getUserId()).thenReturn(42L);
      when(request.getParameter("question")).thenReturn("What is the policy?");
      when(request.getParameter("pageContext")).thenReturn("Current page details.");
      SearchResult result = new SearchResult();
      result.setPageTitle("Policy");
      result.setLink("/policy");
      result.setSourceText("The policy text.");
      WebPage currentPage = new WebPage();
      currentPage.setSearchable(true);
      try (var properties = mockStatic(LoadSitePropertyCommand.class);
          var pages = mockStatic(LoadWebPageCommand.class);
          var repository = mockStatic(UnifiedSearchRepository.class)) {
        pages.when(() -> LoadWebPageCommand.loadByLink("/policy")).thenReturn(currentPage);
        properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("ai.enabled")).thenReturn(true);
        properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("ai.askai.enabled")).thenReturn(true);
        repository.when(() -> UnifiedSearchRepository.findAll(any(), any())).thenAnswer(invocation -> {
          UnifiedSearchSpecification specification = invocation.getArgument(0);
          assertEquals(42L, specification.getForUserId());
          assertTrue(specification.isIncludeSourceText());
          return List.of(result);
        });
        service.post(context);
        verify(context).writeOk(contains("A grounded answer"));
        verify(context).writeOk(contains("/policy"));
        assertEquals("mistral:7b", requestBody.get().path("model").asText());
        assertTrue(requestBody.get().path("messages").get(1).path("content").asText().contains("The policy text."));
        assertTrue(requestBody.get().path("messages").get(1).path("content").asText().contains("Current page details."));
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  void answersFromPageContextWhenSearchIsEmptyAndLimitsItsSize() throws Exception {
    AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    ObjectMapper mapper = new ObjectMapper();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/chat", exchange -> {
      requestBody.set(mapper.readTree(exchange.getRequestBody()));
      byte[] body = "{\"message\":{\"content\":\"Page answer\"}}".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      AskAIJsonService service = spy(new AskAIJsonService());
      doReturn("http://127.0.0.1:" + server.getAddress().getPort()).when(service).getenv("OLLAMA_URL");
      JsonServiceContext context = mock(JsonServiceContext.class);
      PageRequest request = mock(PageRequest.class);
      UserSession session = mock(UserSession.class);
      when(context.getRequest()).thenReturn(request);
      when(request.getHeader("Referer")).thenReturn("https://example.com/policy");
      when(request.getScheme()).thenReturn("https");
      when(request.getServerName()).thenReturn("example.com");
      when(request.getPort()).thenReturn(443);
      when(request.getContextPath()).thenReturn("");
      when(context.getUserSession()).thenReturn(session);
      when(session.getUserId()).thenReturn(42L);
      when(request.getParameter("question")).thenReturn("What is on this page?");
      when(request.getParameter("pageContext")).thenReturn("Page content " + "x".repeat(12000) + "SHOULD NOT APPEAR");
      WebPage currentPage = new WebPage();
      currentPage.setSearchable(true);
      try (var properties = mockStatic(LoadSitePropertyCommand.class);
          var pages = mockStatic(LoadWebPageCommand.class);
          var repository = mockStatic(UnifiedSearchRepository.class)) {
        pages.when(() -> LoadWebPageCommand.loadByLink("/policy")).thenReturn(currentPage);
        properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("ai.enabled")).thenReturn(true);
        properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("ai.askai.enabled")).thenReturn(true);
        repository.when(() -> UnifiedSearchRepository.findAll(any(), any())).thenReturn(List.of());
        service.post(context);
        verify(context).writeOk(contains("Page answer"));
        String prompt = requestBody.get().path("messages").get(1).path("content").asText();
        assertTrue(prompt.contains("Current page (supplementary context):\nPage content"));
        assertFalse(prompt.contains("SHOULD NOT APPEAR"));
        assertTrue(requestBody.get().path("messages").get(0).path("content").asText().contains("untrusted data"));
      }
    } finally {
      server.stop(0);
    }
  }
}