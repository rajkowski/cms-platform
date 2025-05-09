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

package com.simisinc.platform.application.items;

import com.simisinc.platform.domain.model.items.Category;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Methods to display category object information
 *
 * @author matt rajkowski
 * @created 7/29/19 9:09 AM
 */
public class CategoryCommand {

  private static Log LOG = LogFactory.getLog(CategoryCommand.class);

  public static String text(Long categoryId) {
    Category category = LoadCategoryCommand.loadCategoryById(categoryId);
    if (category == null) {
      return null;
    }
    return category.getName();
  }

  public static String icon(Long categoryId) {
    Category category = LoadCategoryCommand.loadCategoryById(categoryId);
    if (category == null) {
      return null;
    }
    if (StringUtils.isBlank(category.getIcon())) {
      return null;
    }
    return category.getIcon();
  }

  public static String headerColorCSS(Long categoryId) {
    Category category = LoadCategoryCommand.loadCategoryById(categoryId);
    if (category == null) {
      return null;
    }
    if (StringUtils.isBlank(category.getHeaderTextColor()) || StringUtils.isBlank(category.getHeaderBgColor())) {
      return null;
    }
    return "background:" + StringEscapeUtils.escapeXml11(category.getHeaderBgColor()) + ";color:" + StringEscapeUtils.escapeXml11(category.getHeaderTextColor());
  }

}
