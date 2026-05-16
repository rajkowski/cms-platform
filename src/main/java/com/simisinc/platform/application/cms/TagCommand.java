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

package com.simisinc.platform.application.cms;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * Methods for working with tags
 *
 * @author matt rajkowski
 * @created 5/16/26 4:51 PM
 */
public class TagCommand {

  /**
   * Returns a string array with unique values
   * @return
   */
  public static String[] normalize(String[] tags) {
    if (tags == null) {
      return null;
    }
    return Arrays.stream(tags)
        .filter(StringUtils::isNotBlank)
        .flatMap(value -> Arrays.stream(value.split(",")))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .distinct()
        .toArray(String[]::new);
  }

  public static String[] normalize(String tags) {
    if (tags == null) {
      return null;
    }
    return Arrays.stream(tags.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .distinct()
        .toArray(String[]::new);
  }
}
