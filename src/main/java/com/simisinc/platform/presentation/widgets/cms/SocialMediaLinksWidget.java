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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Displays a list of social media links
 *
 * @author matt rajkowski
 * @created 1/20/21 3:44 PM
 */
public class SocialMediaLinksWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/social-media-links.jsp";
  static String TEMPLATE = "/cms/social-media-links.html";

  public WidgetContext execute(WidgetContext context) {
    // Use the property map
    Map<String, String> socialPropertyMap = LoadSitePropertyCommand.loadNonEmptyAsMap("social");
    LOG.debug("socialPropertyMap size: " + socialPropertyMap.size());
    if (socialPropertyMap.isEmpty()) {
      return context;
    }
    context.getRequest().setAttribute("socialPropertyMap", socialPropertyMap);

    // Preferences
    context.getRequest().setAttribute("iconClass", context.getPreferences().getOrDefault("iconClass", "margin-left-10"));

    // Create a sort order
    List<String> socialLinks = new ArrayList<>();
    if (socialPropertyMap.containsKey("social.instagram.url")) {
      socialLinks.add("instagram");
    }
    if (socialPropertyMap.containsKey("social.twitter.url")) {
      socialLinks.add("twitter");
    }
    if (socialPropertyMap.containsKey("social.facebook.url")) {
      socialLinks.add("facebook");
    }
    if (socialPropertyMap.containsKey("social.youtube.url")) {
      socialLinks.add("youtube");
    }
    if (socialPropertyMap.containsKey("social.flickr.url")) {
      socialLinks.add("flickr");
    }
    if (socialPropertyMap.containsKey("social.linkedin.url")) {
      socialLinks.add("linkedin");
    }
    context.getRequest().setAttribute("socialLinks", socialLinks);

    // Show the JSP
    context.setJsp(JSP);
    context.setTemplate(TEMPLATE);
    return context;
  }
}
