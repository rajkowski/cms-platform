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

package com.simisinc.platform.presentation.controller;

import java.io.Serializable;

/**
 * Context for rendering a page, tracking counts of rows, columns, and widgets
 *
 * @author matt rajkowski
 * @created 1/18/2026 8:52 AM
 */
public class PageRenderContext implements Serializable {

  static final long serialVersionUID = -8484048371911908893L;

  private int pageRowCount = 0;
  private int pageColumnCount = 0;
  private int pageWidgetCount = 0;

  /**
   * Constructs a PageRenderContext with all counters initialized to zero.
   * This default constructor is used to create an empty context that will be
   * populated with row, column, and widget counts as the page is rendered.
   */
  public PageRenderContext() {
  }

  public int getPageRowCount() {
    return pageRowCount;
  }

  public void setPageRowCount(int pageRowCount) {
    this.pageRowCount = pageRowCount;
  }

  public int getPageColumnCount() {
    return pageColumnCount;
  }

  public void setPageColumnCount(int pageColumnCount) {
    this.pageColumnCount = pageColumnCount;
  }

  public int getPageWidgetCount() {
    return pageWidgetCount;
  }

  public void setPageWidgetCount(int pageWidgetCount) {
    this.pageWidgetCount = pageWidgetCount;
  }

  public int incrementAndGetRowCount() {
    this.pageRowCount++;
    return this.pageRowCount;
  }

  public int incrementAndGetColumnCount() {
    this.pageColumnCount++;
    return this.pageColumnCount;
  }

  public int incrementAndGetWidgetCount() {
    this.pageWidgetCount++;
    return this.pageWidgetCount;
  }

}
