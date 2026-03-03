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

package com.simisinc.platform.domain.model.analytics;

import java.sql.Timestamp;

import com.simisinc.platform.domain.model.Entity;

/**
 * A recorded performance metric for a web request
 *
 * @author matt rajkowski
 * @created 02/27/26 09:00 AM
 */
public class PerformanceMetric extends Entity {

  /** Request type constants */
  public static final String TYPE_PAGE = "page";
  public static final String TYPE_JSON = "json";
  public static final String TYPE_API = "api";

  private long id = -1L;
  private String requestType = null;
  private int statusCode = 200;
  private long durationMs = 0L;
  private Timestamp metricDate = null;

  public PerformanceMetric() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(long durationMs) {
    this.durationMs = durationMs;
  }

  public Timestamp getMetricDate() {
    return metricDate;
  }

  public void setMetricDate(Timestamp metricDate) {
    this.metricDate = metricDate;
  }
}
