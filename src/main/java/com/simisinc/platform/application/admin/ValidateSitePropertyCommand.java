/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.application.admin;

import org.apache.commons.validator.routines.UrlValidator;

import com.simisinc.platform.application.cms.ColorCommand;
import com.simisinc.platform.domain.model.SiteProperty;

/**
 * Functions to validate site property values
 *
 * @author matt rajkowski
 * @created 5/18/24 11:20 AM
 */
public class ValidateSitePropertyCommand {

  public static boolean isValid(SiteProperty siteProperty) {
    return isValid(siteProperty, siteProperty.getValue());
  }

  public static boolean isValid(SiteProperty siteProperty, String valueToCheck) {
    if ("color".equals(siteProperty.getType())) {
      return isValidColor(valueToCheck);
    } else if ("url".equals(siteProperty.getType())) {
      return isValidUrl(valueToCheck);
    }
    return true;
  }

  public static boolean isValidColor(String value) {
    return ColorCommand.isHexColor(value);
  }

  public static boolean isValidUrl(String value) {
    String[] schemes = { "http", "https" };
    UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
    return urlValidator.isValid(value);
  }

}
