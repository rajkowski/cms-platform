package com.simisinc.platform.infrastructure.persistence.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.zeroio.platform.application.ai.EmbeddingCommand;
import com.zeroio.platform.domain.model.cms.SearchCriteria;
import com.zeroio.platform.infrastructure.persistence.cms.SearchChunkRepository;

class UnifiedSearchRepositoryTest {

  @Test
  void removesConversationalPrefixesFromLexicalQueries() {
    assertEquals("the economic profile", UnifiedSearchRepository.lexicalSearchTerm("tell me about the economic profile"));
    assertEquals("the economic profile?", UnifiedSearchRepository.lexicalSearchTerm("What is the economic profile?"));
    assertEquals("economic profile", UnifiedSearchRepository.lexicalSearchTerm("CAN YOU TELL ME ABOUT economic profile"));
  }

  @Test
  void preservesNormalQueriesAndSearchOperators() {
    assertEquals("economic profile", UnifiedSearchRepository.lexicalSearchTerm("economic profile"));
    assertEquals("\"tell me about the economic profile\"", UnifiedSearchRepository.lexicalSearchTerm("\"tell me about the economic profile\""));
    assertEquals("economic OR profile -report", UnifiedSearchRepository.lexicalSearchTerm("economic OR profile -report"));
    assertEquals("tell me about", UnifiedSearchRepository.lexicalSearchTerm("tell me about"));
  }

  @Test
  void expandsAttachmentsToIncludeResourceFiles() {
    assertEquals(List.of(SearchCriteria.ATTACHMENTS, SearchCriteria.RESOURCE_FILES),
        List.of(UnifiedSearchRepository.expandAttachmentTypes(new String[] { SearchCriteria.ATTACHMENTS })));
    assertEquals(List.of(SearchCriteria.RESOURCE_FILES),
        List.of(UnifiedSearchRepository.expandAttachmentTypes(new String[] { SearchCriteria.RESOURCE_FILES })));
  }

  @Test
  void combinesOriginalQuestionEmbeddingWithRelaxedLexicalQuery() throws Exception {
    String question = "tell me about the economic profile";
    float[] vector = { 0.1f };
    Select select = mock(Select.class, RETURNS_SELF);
    DataResult<SearchResult> data = new DataResult<>();
    data.setRecords(List.of());
    when(select.returnDataResult(ArgumentMatchers.<DB.ResultSetMapper<SearchResult>>any())).thenReturn(data);
    UnifiedSearchSpecification specification = new UnifiedSearchSpecification();
    specification.setSearchTerm(question);
    try (var database = mockStatic(DB.class);
        var embeddings = mockStatic(EmbeddingCommand.class);
        var chunks = mockStatic(SearchChunkRepository.class);
        var properties = mockStatic(LoadSitePropertyCommand.class)) {
        database.when(() -> DB.SELECT("result_type", "result_id", "title", "description", "link", "related_category_id", "category", "tags",
          "created", "modified", "modified_by", "file_name", "file_type", "mime_type", "extension", "custom_field_values")).thenReturn(select);
      embeddings.when(EmbeddingCommand::isAvailable).thenReturn(true);
      embeddings.when(() -> EmbeddingCommand.embed(List.of(question))).thenReturn(List.of(vector));
      chunks.when(() -> SearchChunkRepository.findNearestSources(vector, EmbeddingCommand.MODEL_NAME, 200, 20, 0.60))
          .thenReturn(List.of("pages:42"));

      UnifiedSearchRepository.findAll(specification, null);

      embeddings.verify(() -> EmbeddingCommand.embed(List.of(question)));
      verify(select).AND(
          "(tsv @@ websearch_to_tsquery('english', ?) OR tsv @@ " + UnifiedSearchRepository.relaxedLexicalQuery()
              + " OR (result_type || ':' || result_id) = ANY(ARRAY['pages:42']))",
          "the economic profile", "the economic profile");
    }
  }

  @Test
  void usesRelaxedLexicalQueryWhenVectorsAreUnavailable() {
    Select select = mock(Select.class, RETURNS_SELF);
    DataResult<SearchResult> data = new DataResult<>();
    data.setRecords(List.of());
    when(select.returnDataResult(ArgumentMatchers.<DB.ResultSetMapper<SearchResult>>any())).thenReturn(data);
    UnifiedSearchSpecification specification = new UnifiedSearchSpecification();
    specification.setSearchTerm("tell me about the economic profile");
    try (var database = mockStatic(DB.class);
        var embeddings = mockStatic(EmbeddingCommand.class);
        var properties = mockStatic(LoadSitePropertyCommand.class)) {
        database.when(() -> DB.SELECT("result_type", "result_id", "title", "description", "link", "related_category_id", "category", "tags",
          "created", "modified", "modified_by", "file_name", "file_type", "mime_type", "extension", "custom_field_values")).thenReturn(select);
      embeddings.when(EmbeddingCommand::isAvailable).thenReturn(false);

      UnifiedSearchRepository.findAll(specification, null);

      verify(select).AND(
          "(tsv @@ websearch_to_tsquery('english', ?) OR tsv @@ " + UnifiedSearchRepository.relaxedLexicalQuery() + ")",
          "the economic profile", "the economic profile");
    }
  }

  @Test
  void prioritizesKeywordMatchesOverSemanticOnlyResults() {
    String sql = UnifiedSearchRepository.relevanceSql("ARRAY['pages:42']");

    assertEquals(
        "(ts_rank_cd(tsv, websearch_to_tsquery('english', ?), 32)"
            + " + CASE WHEN tsv @@ websearch_to_tsquery('english', ?) THEN 1.0 ELSE 0.0 END"
            + " + ts_rank_cd(tsv, " + UnifiedSearchRepository.relaxedLexicalQuery() + ", 32)"
            + " + CASE WHEN LOWER(title) = ? THEN 2.0 ELSE 0.0 END"
            + " + CASE WHEN LOWER(title) LIKE ? ESCAPE '!' THEN 1.0 ELSE 0.0 END"
            + " + CASE WHEN LOWER(title) LIKE ? ESCAPE '!' THEN 0.5 ELSE 0.0 END"
            + " + COALESCE(0.5 / (10 + array_position(ARRAY['pages:42'], result_type || ':' || result_id)), 0.0)) AS rank",
        sql);
  }

  @Test
  void createsStemmedOrQueryForPartialTermMatches() {
    assertEquals(
        "to_tsquery('english', replace(plainto_tsquery('english', ?)::text, ' & ', ' | '))",
        UnifiedSearchRepository.relaxedLexicalQuery());
  }
}