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

package com.simisinc.platform.application.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;

/**
 * Describes the site and theme properties supported by the visual workspace editor.
 */
public class VisualWorkspacePropertyCatalog {

  private static final Set<String> EDITABLE_SITE_PROPERTY_NAMES = Set.of(
      "site.name",
      "site.name.keyword",
      "site.description",
      "site.keywords",
      "site.image",
      "site.header.line1",
      "site.header.link",
      "site.header.page",
      "site.footer.line1",
      "site.footer.line2",
      "site.privacy.policy",
      "site.terms.conditions",
      "site.timezone",
      "site.confirmation",
      "site.confirmation.line1",
      "site.confirmation.line2",
      "site.confirmation.declined.text",
      "site.newsletter.overlay",
      "site.newsletter.headline",
      "site.newsletter.message",
      "site.newsletter.color",
      "site.newsletter.backgroundColor",
      "site.logo",
      "site.logo.mixed",
      "site.logo.white");

  public static List<SiteProperty> loadEditableProperties() {
    List<SiteProperty> properties = new ArrayList<>();
    addEditableProperties(properties, SitePropertyRepository.findAllByPrefix("site"));
    addEditableProperties(properties, SitePropertyRepository.findAllByPrefix("theme"));
    return properties;
  }

  static boolean isEditableProperty(SiteProperty property) {
    if (property == null || property.getName() == null || "disabled".equals(property.getType())
        || hasEnvironmentOverride(property.getName())) {
      return false;
    }
    if (EDITABLE_SITE_PROPERTY_NAMES.contains(property.getName())) {
      return true;
    }
    return property.getName().startsWith("theme.") && "color".equals(property.getType())
        && getPreviewTarget(property.getName()) != null;
  }

  public static String getCategory(SiteProperty property) {
    String name = property.getName();
    if (name.startsWith("theme.")) {
      String target = getPreviewTarget(name);
      if ("body".equals(target)) {
        return "Theme: Body";
      } else if ("utilitybar".equals(target)) {
        return "Theme: Utility Bar";
      } else if ("topbar".equals(target)) {
        return "Theme: Navigation";
      } else if ("button".equals(target)) {
        return "Theme: Buttons";
      } else if ("callout".equals(target)) {
        return "Theme: Callouts";
      } else if ("pagination".equals(target)) {
        return "Theme: Pagination";
      } else if ("label".equals(target)) {
        return "Theme: Labels";
      } else if ("badge".equals(target)) {
        return "Theme: Badges";
      } else if ("progress".equals(target)) {
        return "Theme: Progress Meters";
      } else if ("footer".equals(target)) {
        return "Theme: Footer";
      }
    } else if (name.startsWith("site.header.")) {
      return "Site: Header";
    } else if (name.startsWith("site.footer.")) {
      return "Site: Footer";
    } else if (name.startsWith("site.newsletter.")) {
      return "Site: Newsletter";
    } else if (name.startsWith("site.logo")) {
      return "Site: Logos";
    } else if (name.startsWith("site.confirmation")) {
      return "Site: Visitor Confirmation";
    } else if ("site.timezone".equals(name)) {
      return "Site: Locale";
    } else if ("site.image".equals(name)) {
      return "Site: Search and Sharing";
    }
    return "Site: Identity";
  }

  public static String getPreviewTarget(String propertyName) {
    if (propertyName.startsWith("theme.body.")) {
      return "body";
    } else if (propertyName.startsWith("theme.utilitybar.")) {
      return "utilitybar";
    } else if (propertyName.startsWith("theme.topbar.")) {
      return "topbar";
    } else if (propertyName.startsWith("theme.button.")) {
      return "button";
    } else if (propertyName.startsWith("theme.callout.")) {
      return "callout";
    } else if (propertyName.startsWith("theme.pagination.")) {
      return "pagination";
    } else if (propertyName.startsWith("theme.label.")) {
      return "label";
    } else if (propertyName.startsWith("theme.badge.")) {
      return "badge";
    } else if (propertyName.startsWith("theme.progress.")) {
      return "progress";
    } else if (propertyName.startsWith("theme.footer.")) {
      return "footer";
    }
    return null;
  }

  private static void addEditableProperties(List<SiteProperty> result, List<SiteProperty> properties) {
    if (properties == null) {
      return;
    }
    for (SiteProperty property : properties) {
      if (isEditableProperty(property)) {
        result.add(property);
      }
    }
  }

  private static boolean hasEnvironmentOverride(String propertyName) {
    String environmentName = "CMS_" + propertyName.replace('.', '_').toUpperCase();
    return System.getenv(environmentName) != null;
  }
}
