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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.dashboard.ActiveSessionData;
import com.simisinc.platform.infrastructure.persistence.SessionRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHitRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * AJAX endpoint for analytics live data loading
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class AnalyticsLiveLoadAjax extends GenericWidget {
  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(AnalyticsLiveLoadAjax.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public WidgetContext execute(WidgetContext context) {
    try {
      // Build response
      ObjectNode response = MAPPER.createObjectNode();
      response.put("success", true);
      response.put("generatedAt", System.currentTimeMillis());
      
      // Get active sessions array
      var sessionsArray = response.putArray("activeSessions");
      List<ActiveSessionData> activeSessions = SessionRepository.findActiveSessions(20, 10);
      if (activeSessions != null) {
        for (ActiveSessionData session : activeSessions) {
          var sessionObj = sessionsArray.addObject();
          sessionObj.put("sessionId", session.getSessionId());
          sessionObj.put("userType", session.getUserType());
          sessionObj.put("page", session.getPage());
          sessionObj.put("device", session.getDevice());
          sessionObj.put("location", session.getLocation());
          sessionObj.put("duration", session.getDuration());
        }
      }
      
      // Get top pages from last 1 hour for recent events
      var eventsArray = response.putArray("recentEvents");
      var topPages = WebPageHitRepository.findTopPaths(1, 'h', 5);
      if (topPages != null) {
        int idx = 0;
        for (var page : topPages) {
          var eventObj = eventsArray.addObject();
          long timestamp = System.currentTimeMillis() - (idx * 60000); // Spread across last hour
          eventObj.put("timestamp", timestamp);
          eventObj.put("page", page.getLabel());
          eventObj.put("type", "pageview");
          eventObj.put("views", Long.parseLong(page.getValue()));
          idx++;
        }
      }
      
      context.setJson(response.toString());
    } catch (Exception e) {
      LOG.error("Error loading live analytics", e);
      context.setSuccess(false);
    }
    return context;
  }
}
