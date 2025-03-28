/*
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

package com.simisinc.platform.presentation.widgets.cms;

import com.simisinc.platform.WidgetBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author matt rajkowski
 * @created 5/7/2022 8:30 AM
 */
class WebPageSearchResultsWidgetTest extends WidgetBase {

  @Test
  void execute() {
    // No query parameters
    WebPageSearchResultsWidget widget = new WebPageSearchResultsWidget();
    Assertions.assertNull(widget.execute(widgetContext));

    // Set widget preferences
    preferences.put("query", "test");

    // Expect no results
    widget.execute(widgetContext);
    Assertions.assertNull(widgetContext.getJsp());

    // Mock the results
//    Assertions.assertEquals(JSP, widgetContext.getJsp());
  }
}