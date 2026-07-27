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
package com.zeroio.platform.domain.events.cms;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.domain.events.Event;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.UserRepository;

import lombok.NoArgsConstructor;

/**
 * Event details for when web page draft content is edited
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
@NoArgsConstructor
public class WebPageDraftContentEditedEvent extends Event {

  public static final String ID = "web-page-draft-content-edited";

  private WebPage webPage = null;
  private long userId = -1L;

  public WebPageDraftContentEditedEvent(WebPage webPage, long userId) {
    this.webPage = webPage;
    this.userId = userId;
  }

  @Override
  public String getDomainEventType() {
    return ID;
  }

  public User getUser() {
    return UserRepository.findByUserId(userId);
  }

  public void setWebPage(WebPage webPage) {
    this.webPage = webPage;
  }

  public WebPage getWebPage() {
    return webPage;
  }

  public String getTitle() {
    if (StringUtils.isNotBlank(webPage.getTitle())) {
      return webPage.getTitle();
    } else if ("/".equals(webPage.getLink())) {
      return "Home page";
    } else {
      // The user may not have set a title
      return webPage.getLink();
    }
  }
}
