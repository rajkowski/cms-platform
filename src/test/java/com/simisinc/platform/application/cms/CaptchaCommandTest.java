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

package com.simisinc.platform.application.cms;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.presentation.controller.SessionConstants;

/**
 * @author matt rajkowski
 * @created 5/3/2022 7:00 PM
 */
class CaptchaCommandTest extends WidgetBase {

  @Test
  void validateRequest() {

    try (MockedStatic<LoadSitePropertyCommand> property = mockStatic(LoadSitePropertyCommand.class)) {
      property.when(() -> LoadSitePropertyCommand.loadByName(anyString())).thenReturn(null);

      // Set the correct captcha value
      session.setAttribute(SessionConstants.CAPTCHA_TEXT, "12345");

      // Test when a captcha is not submitted
      Assertions.assertFalse(CaptchaCommand.validateRequest(widgetContext));

      // Test when a captcha is submitted correctly
      addQueryParameter(widgetContext, "captcha", "12345");
      Assertions.assertTrue(CaptchaCommand.validateRequest(widgetContext));

      // Test when a captcha is submitted incorrectly
      addQueryParameter(widgetContext, "captcha", "00000");
      Assertions.assertFalse(CaptchaCommand.validateRequest(widgetContext));
    }
  }

  @Test
  void generateImage() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Assertions.assertEquals(0, out.size());
    try {
      CaptchaCommand.generateImage("test", out);
      out.close();
      Assertions.assertNotNull(out);
    } catch (Exception e) {
      fail("Should not have thrown any exception");
    }
    Assertions.assertTrue(out.size() > 0);
  }
}