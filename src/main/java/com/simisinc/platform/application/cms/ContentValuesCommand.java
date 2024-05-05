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

package com.simisinc.platform.application.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods for replacing dynamic properties in content
 *
 * @author matt rajkowski
 * @created 2/24/24 11:19 AM
 */
public class ContentValuesCommand {

  private static Log LOG = LogFactory.getLog(ContentValuesCommand.class);

  public static String replaceDynamicValues(String content) {

    // It's possible to have different content injected into this content, so process it
    // content = embedInlineContent(context, content);
    
    content = ThemeValuesCommand.replaceThemeDynamicValues(content);

    return content;
  }

}
