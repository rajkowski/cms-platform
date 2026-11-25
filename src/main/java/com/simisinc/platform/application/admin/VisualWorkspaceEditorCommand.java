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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;

/**
 * Loads and saves the values available in the visual workspace editor.
 */
public class VisualWorkspaceEditorCommand {

  private static final Log LOG = LogFactory.getLog(VisualWorkspaceEditorCommand.class);

  public static List<SiteProperty> loadEditableProperties() {
    return VisualWorkspacePropertyCatalog.loadEditableProperties();
  }

  public static SaveResult saveValues(Map<String, String> submittedValues) {
    return saveValues(submittedValues, null);
  }

  public static SaveResult saveValues(Map<String, String> submittedValues, String allowedPrefix) {
    List<SiteProperty> currentProperties = loadEditableProperties();
    Map<String, SiteProperty> currentByName = new LinkedHashMap<>();
    for (SiteProperty property : currentProperties) {
      currentByName.put(property.getName(), property);
    }

    Map<String, String> errors = new LinkedHashMap<>();
    List<SiteProperty> updates = new ArrayList<>();
    if (submittedValues == null) {
      errors.put("_global", "No property values were submitted.");
      return new SaveResult(false, errors);
    }

    for (Map.Entry<String, String> entry : submittedValues.entrySet()) {
      String name = entry.getKey();
      SiteProperty property = currentByName.get(name);
      if (property == null || "site.url".equals(name) || (allowedPrefix != null && !name.startsWith(allowedPrefix))) {
        errors.put(name, "This property cannot be changed in this visual editor.");
        continue;
      }

      String value = normalizeValue(property, entry.getValue());
      String validationError = validateValue(property, value);
      if (validationError != null) {
        errors.put(name, validationError);
        continue;
      }

      if (!Objects.equals(property.getValue(), value)) {
        property.setValue(value);
        updates.add(property);
      }
    }

    if (!errors.isEmpty()) {
      return new SaveResult(false, errors);
    }
    if (updates.isEmpty()) {
      return new SaveResult(true, errors);
    }

    if (!SitePropertyRepository.saveAll("site,theme", updates)) {
      LOG.error("One or more visual workspace editor values could not be saved");
      CacheManager.invalidateCurrentWorkspaceKey(CacheManager.SYSTEM_PROPERTY_PREFIX_CACHE, "site", true);
      CacheManager.invalidateCurrentWorkspaceKey(CacheManager.SYSTEM_PROPERTY_PREFIX_CACHE, "theme", true);
      errors.put("_global", "The values could not all be saved. Your changes are still available to retry.");
      return new SaveResult(false, errors);
    }
    return new SaveResult(true, errors);
  }

  private static String normalizeValue(SiteProperty property, String submittedValue) {
    String value = StringUtils.trimToEmpty(submittedValue);
    if ("web-page".equals(property.getType()) && StringUtils.isNotBlank(value) && !value.startsWith("/")) {
      return "/" + value;
    }
    return value;
  }

  private static String validateValue(SiteProperty property, String value) {
    if ("boolean".equals(property.getType()) && !"true".equals(value) && !"false".equals(value)) {
      return property.getLabel() + " must be Yes or No.";
    }
    if ("timezone".equals(property.getType()) && StringUtils.isNotBlank(value)) {
      try {
        ZoneId.of(value);
      } catch (DateTimeException exception) {
        return property.getLabel() + " must be a valid time zone.";
      }
    }
    if (!ValidateSitePropertyCommand.isValid(property, value)) {
      return property.getLabel() + " has an invalid value.";
    }
    return null;
  }

  public static class SaveResult {
    private final boolean successful;
    private final Map<String, String> errors;

    private SaveResult(boolean successful, Map<String, String> errors) {
      this.successful = successful;
      this.errors = errors;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public Map<String, String> getErrors() {
      return errors;
    }
  }
}
