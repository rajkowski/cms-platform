/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.rest.services.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.LoadContentCommand;
import com.simisinc.platform.application.cms.SaveContentCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.simisinc.platform.rest.controller.ServiceResponseCommand;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 4/17/18 9:00 AM
 */
public class ContentService extends GenericRestService {

  private static Log LOG = LogFactory.getLog(ContentService.class);

  // GET /content/{contentUniqueId}
  @Override
  public ServiceResponse get(ServiceContext context) {

    String contentUniqueId = context.getPathParam();
    Content content = LoadContentCommand.loadContentByUniqueId(contentUniqueId);
    if (content == null) {
      ServiceResponse response = new ServiceResponse(404);
      response.getError().put("title", "Content was not found");
      return response;
    }

    // Set the fields to return
    ContentResponse contentResponse = new ContentResponse(content);

    // Prepare the response
    ServiceResponse response = new ServiceResponse(200);
    ServiceResponseCommand.addMeta(response, "content");
    response.setData(contentResponse);
    return response;
  }

  // POST /content
  @Override
  public ServiceResponse post(ServiceContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      ServiceResponse response = new ServiceResponse(403);
      response.getError().put("title", "Not authorized");
      return response;
    }

    String contentUniqueId = StringUtils.trimToNull(context.getParameter("contentUniqueId"));
    String contentHtml = context.getParameter("content");
    boolean isDraft = "true".equalsIgnoreCase(context.getParameter("isDraft"));

    if (StringUtils.isBlank(contentUniqueId)) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Content unique ID is required");
      return response;
    }

    if (StringUtils.isBlank(contentHtml)) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Content is required");
      return response;
    }

    try {
      Content contentBean = new Content();
      contentBean.setUniqueId(contentUniqueId);
      contentBean.setCreatedBy(context.getUserId());
      contentBean.setModifiedBy(context.getUserId());
      if (isDraft) {
        contentBean.setDraftContent(contentHtml);
      } else {
        contentBean.setContent(contentHtml);
      }

      Content content = SaveContentCommand.saveContent(contentBean, isDraft);
      if (content == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put("title", "The content could not be saved");
        return response;
      }

      ServiceResponse response = new ServiceResponse(200);
      ServiceResponseCommand.addMeta(response, "content");
      response.setData(new ContentResponse(content));
      return response;
    } catch (DataException e) {
      LOG.error("saveError", e);
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", e.getMessage());
      return response;
    }
  }

}
