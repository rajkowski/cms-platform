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
package com.simisinc.platform.presentation.widgets.items;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
class ItemsListWidgetTest {

  @Test
  void parseInFilterValues_quotedValues() {
    List<String> result = ItemsListWidget.parseInFilterValues(
        "\"global_metrics\",\"region1_metrics\",\"region2_metrics\"");
    Assertions.assertEquals(3, result.size());
    Assertions.assertEquals("global_metrics", result.get(0));
    Assertions.assertEquals("region1_metrics", result.get(1));
    Assertions.assertEquals("region2_metrics", result.get(2));
  }

  @Test
  void parseInFilterValues_unquotedValues() {
    List<String> result = ItemsListWidget.parseInFilterValues("val1,val2,val3");
    Assertions.assertEquals(3, result.size());
    Assertions.assertEquals("val1", result.get(0));
    Assertions.assertEquals("val2", result.get(1));
    Assertions.assertEquals("val3", result.get(2));
  }

  @Test
  void parseInFilterValues_singleValue() {
    List<String> result = ItemsListWidget.parseInFilterValues("\"global_metrics\"");
    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals("global_metrics", result.get(0));
  }

  @Test
  void parseInFilterValues_emptyString() {
    List<String> result = ItemsListWidget.parseInFilterValues("");
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void parseInFilterValues_nullValue() {
    List<String> result = ItemsListWidget.parseInFilterValues(null);
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void parseInFilterValues_withSpaces() {
    List<String> result = ItemsListWidget.parseInFilterValues(
        "\"global_metrics\", \"region1_metrics\", \"region2_metrics\"");
    Assertions.assertEquals(3, result.size());
    Assertions.assertEquals("global_metrics", result.get(0));
    Assertions.assertEquals("region1_metrics", result.get(1));
    Assertions.assertEquals("region2_metrics", result.get(2));
  }

  @Test
  void parseInFilterValues_fullSpecExample() {
    List<String> result = ItemsListWidget.parseInFilterValues(
        "\"global_metrics\",\"region1_metrics\",\"region2_metrics\",\"region3_metrics\",\"region4_metrics\",\"region5_metrics\"");
    Assertions.assertEquals(6, result.size());
    Assertions.assertEquals("global_metrics", result.get(0));
    Assertions.assertEquals("region5_metrics", result.get(5));
  }
}
