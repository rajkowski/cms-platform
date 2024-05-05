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

package com.simisinc.platform.rest.services.cms;

import com.simisinc.platform.application.cms.ContentValuesCommand;
import com.simisinc.platform.domain.model.cms.Content;

/**
 * The allowed fields used in a service call
 *
 * @author matt rajkowski
 * @created 1/22/19 12:12 PM
 */
public class ContentResponse {

  String uniqueId;
  String content;

  public ContentResponse(Content thisContent) {
    uniqueId = thisContent.getUniqueId();
    content = ContentValuesCommand.replaceDynamicValues(thisContent.getContent());
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
