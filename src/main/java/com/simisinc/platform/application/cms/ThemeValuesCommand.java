/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;

/**
 * Methods for working with theme values
 *
 * @author matt rajkowski
 * @created 2/24/2024 10:58 AM
 */
public class ThemeValuesCommand {

  private static final String prefix = "theme";
  private static Log LOG = LogFactory.getLog(ThemeValuesCommand.class);

  /**
   * Replace all theme properties in the given content
   * @param content
   */
  public static String replaceThemeDynamicValues(String content) {
    // Use the list of the current properties
    List<SiteProperty> siteProperties = SitePropertyRepository.findAllByPrefix(prefix);
    if (siteProperties == null || siteProperties.isEmpty()) {
      return content;
    }
    // Replace corresponding properties
    for (SiteProperty property : siteProperties) {
      content = StringUtils.replace(content, "${" + property.getName() + "}", property.getValue());
    }
    return content;
  }
}
