/*
 * Copyright 2026 Matt Rajkowski
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.simisinc.platform.presentation.widgets.cms.SaveWebPageAjax;
import com.zeroio.platform.application.cms.SetCurrentWebPageVersionCommand;

/**
 * Reverts a web page XML layout to a previous version
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class WebPageVersionRevertAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908898L;
  private static Log LOG = LogFactory.getLog(WebPageVersionRevertAjax.class);

  @Override
  public JsonServiceContext post(JsonServiceContext context) {

    LOG.debug("WebPageVersionRevertAjax...");

    // Reuse the same permission gate as the visual page save endpoint.
    if (!PermissionEngine.checkAccess("cms.web-page.save", context.getUserSession())) {
      LOG.debug("No permission to: " + WebPageVersionRevertAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long webPageId = context.getParameterAsLong("webPageId", -1);
    long versionId = context.getParameterAsLong("versionId", -1);

    if (webPageId == -1 || versionId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Web page ID and Version ID required\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      WebPage page = SetCurrentWebPageVersionCommand.setCurrentVersion(webPageId, versionId, context.getUserId());
      if (page != null) {
        context.setJson("{\"success\": true, \"message\": \"Page version restored\", \"webPageId\": " + page.getId() + "}");
        return context;
      }
      return context.writeError("Failed to update page");
    } catch (DataException e) {
      LOG.error("Error reverting page version", e);
      return context.writeError(e.getMessage());
    } catch (Exception e) {
      LOG.error("Error reverting page version", e);
      return context.writeError("Failed to revert: " + e.getMessage());
    }
  }
}