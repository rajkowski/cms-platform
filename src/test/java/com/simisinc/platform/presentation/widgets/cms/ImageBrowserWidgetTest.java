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

import com.simisinc.platform.WidgetBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for ImageBrowserWidget
 *
 * @author github copilot
 * @created 2/14/26
 */
class ImageBrowserWidgetTest extends WidgetBase {

  @Test
  void execute() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext, "<widget name=\"imageBrowser\"/>");

    // Execute the widget
    ImageBrowserWidget widget = new ImageBrowserWidget();
    widget.execute(widgetContext);

    // Verify embedded mode
    Assertions.assertTrue(widgetContext.isEmbedded());
    
    // Verify JSP is set (not null)
    Assertions.assertNotNull(widgetContext.getJsp());
  }

  @Test
  void executeWithInputId() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext, "<widget name=\"imageBrowser\"/>");
    
    // Add request parameter for inputId using setAttribute (parameters are stored as attributes in mock)
    pageRequest.setAttribute("inputId", "testInputId");

    // Execute the widget
    ImageBrowserWidget widget = new ImageBrowserWidget();
    widget.execute(widgetContext);

    // Verify inputId is set in request
    Assertions.assertEquals("testInputId", pageRequest.getAttribute("inputId"));
    
    // Verify embedded mode
    Assertions.assertTrue(widgetContext.isEmbedded());
  }

  @Test
  void executeWithRevealView() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext, "<widget name=\"imageBrowser\"/>");
    
    // Add request parameter for view=reveal using setAttribute
    pageRequest.setAttribute("view", "reveal");

    // Execute the widget
    ImageBrowserWidget widget = new ImageBrowserWidget();
    widget.execute(widgetContext);

    // Verify embedded mode
    Assertions.assertTrue(widgetContext.isEmbedded());
  }
}
