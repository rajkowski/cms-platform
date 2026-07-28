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
package com.zeroio.platform.rest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.WordUtils;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveWebPageCommand;
import com.simisinc.platform.application.cms.WebPageJsonToXMLCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;

/**
 * Gets, creates or updates web pages from posted request parameters.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class WebPageService extends GenericRestService {

  private static Log LOG = LogFactory.getLog(WebPageService.class);

  // POST /webPage
  @Override
  public ServiceResponse post(ServiceContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      ServiceResponse response = new ServiceResponse(403);
      response.getError().put("title", "Not authorized");
      return response;
    }

    WebPage webPage = loadExistingWebPage(context);
    if (webPage == null) {
      webPage = new WebPage();
    }

    try {
      BeanUtils.populate(webPage, context.getParameterMap());

      String webPageLink = StringUtils.trimToNull(context.getParameter("webPageLink"));
      if (webPageLink == null) {
        webPageLink = StringUtils.trimToNull(context.getParameter("webPage"));
      }
      if (webPageLink != null) {
        webPage.setLink(webPageLink);
      }

      String designerData = context.getParameter("designerData");
      if (StringUtils.isBlank(webPage.getPageXml()) && StringUtils.isNotBlank(designerData)) {
        webPage.setPageXml(WebPageJsonToXMLCommand.convertDesignerJsonToXml(designerData));
      }

      if (StringUtils.isBlank(webPage.getLink())) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put("title", "A web page link is required");
        return response;
      }

      if (StringUtils.isBlank(webPage.getTitle())) {
        String title = webPage.getLink().replace("-", " ");
        title = StringUtils.substringAfterLast(title, "/");
        webPage.setTitle(WordUtils.capitalizeFully(title, ' '));
      }

      webPage.setCreatedBy(context.getUserId());
      webPage.setModifiedBy(context.getUserId());

      WebPage savedWebPage = SaveWebPageCommand.saveWebPage(webPage);
      if (savedWebPage == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put("title", "The web page could not be saved");
        return response;
      }

      ServiceResponse response = new ServiceResponse(200);
      response.getMeta().put("type", "webPage");
      response.getMeta().put("id", savedWebPage.getId());
      response.setData(savedWebPage);
      return response;
    } catch (DataException e) {
      LOG.error("saveError", e);
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", e.getMessage());
      return response;
    } catch (Exception e) {
      LOG.error("postError", e);
      ServiceResponse response = new ServiceResponse(500);
      response.getError().put("title", "An unexpected error occurred saving the web page");
      return response;
    }
  }

  private static WebPage loadExistingWebPage(ServiceContext context) {
    String pathParam = context.getPathParam();
    if (StringUtils.isNumeric(pathParam)) {
      WebPage webPage = WebPageRepository.findById(Long.parseLong(pathParam));
      if (webPage != null) {
        return webPage;
      }
    }

    String webPageId = context.getParameter("id");
    if (StringUtils.isNumeric(webPageId)) {
      WebPage webPage = WebPageRepository.findById(Long.parseLong(webPageId));
      if (webPage != null) {
        return webPage;
      }
    }

    String webPageLink = StringUtils.trimToNull(context.getParameter("webPageLink"));
    if (webPageLink == null) {
      webPageLink = StringUtils.trimToNull(context.getParameter("webPage"));
    }
    if (webPageLink != null) {
      return WebPageRepository.findByLink(webPageLink);
    }

    return null;
  }

}
