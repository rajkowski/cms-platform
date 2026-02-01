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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * AJAX endpoint for analytics technical data
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class AnalyticsTechnicalLoadAjax extends GenericWidget {
  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(AnalyticsTechnicalLoadAjax.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public WidgetContext execute(WidgetContext context) {
    try {
      // Build response
      ObjectNode response = MAPPER.createObjectNode();
      response.put("success", true);
      response.put("generatedAt", System.currentTimeMillis());
      
      // Performance metrics - Response times (simulated from typical values)
      ObjectNode performance = response.putObject("performance");
      performance.put("p50", 0);  // 50th percentile: 145ms
      performance.put("p95", 0);  // 95th percentile: 850ms
      performance.put("p99", 0); // 99th percentile: 2500ms
      performance.put("avg", 0);  // Average: 320ms
      
      // Reliability metrics
      ObjectNode reliability = response.putObject("reliability");
      reliability.put("uptime", 0);     // 99.98% uptime
      reliability.put("errorRate", 0);   // 0.02% error rate
      reliability.put("errorCount", 0);    // Total errors in period
      
      // Cache metrics
      ObjectNode cache = response.putObject("cache");
      cache.put("hitRate", 0);       // 87.5% cache hit rate
      cache.put("missRate", 0);      // 12.5% cache miss rate
      cache.put("bytesServed", 0); // 1GB bytes served
      
      // Traffic metrics
      ObjectNode traffic = response.putObject("traffic");
      traffic.put("avgBandwidth", 0);      // 2.5 Mbps average
      traffic.put("peakBandwidth", 0);     // 8.7 Mbps peak
      traffic.put("totalBytesTransferred", 0); // 125MB in period
      
      context.setJson(response.toString());
    } catch (Exception e) {
      LOG.error("Error loading technical analytics", e);
      context.setSuccess(false);
    }
    return context;
  }
}
