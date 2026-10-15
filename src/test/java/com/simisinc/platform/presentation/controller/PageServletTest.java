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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PageServletTest {

  @Test
  void headerSelectionPreservesDefaultAndSpecialRouteRules() {
    assertEquals("header.default", PageServlet.selectHeaderName("/", "/", null, Map.of()));
    assertEquals("header.custom", PageServlet.selectHeaderName("/", "/", null, Map.of("theme.header.name", "header.custom")));
    assertEquals("header.plain",
        PageServlet.selectHeaderName("/checkout", "/checkout", null, Map.of("theme.header.name", "header.default")));
    assertEquals("header.default", PageServlet.selectHeaderName("/admin/users", "/admin/users", null, Map.of()));
    assertNull(PageServlet.selectHeaderName("/", "/", "iframe", Map.of()));
    assertNull(PageServlet.selectHeaderName("/admin/system", "/admin/system", null, Map.of()));
    assertNull(PageServlet.selectHeaderName("/admin/visual-editor", "/admin/visual-editor", null, Map.of()));
  }
}