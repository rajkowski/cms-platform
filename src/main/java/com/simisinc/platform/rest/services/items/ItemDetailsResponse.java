/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.rest.services.items;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;

/**
 * Item Response
 *
 * @author matt rajkowski
 * @created 1/22/19 12:12 PM
 */
@JsonPropertyOrder({ "uniqueId", "name" })
public class ItemDetailsResponse {

  String uniqueId;
  String name;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String summary;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String description;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String location;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String street;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String city;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String state;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String postalCode;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  Double latitude;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  Double longitude;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String imageUrl;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String barcode;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String[] tags;
  private Map<String, String> customFields;

  public ItemDetailsResponse(Item record, Collection collection) {
    uniqueId = record.getUniqueId();
    name = record.getName();
    summary = record.getSummary();
    description = record.getDescription();
    location = record.getLocation();
    street = record.getStreet();
    city = record.getCity();
    state = record.getState();
    postalCode = record.getPostalCode();
    if (record.hasGeoPoint()) {
      latitude = record.getLatitude();
      longitude = record.getLongitude();
    }
    imageUrl = record.getImageUrl();
    barcode = record.getBarcode();
    tags = record.getTags();
    if (collection.getCustomFieldList() != null && !collection.getCustomFieldList().isEmpty()
        && record.getCustomFieldList() != null && !record.getCustomFieldList().isEmpty()) {
      customFields = new LinkedHashMap<>();
      for (CustomField fieldDef : collection.getCustomFieldList().values()) {
        CustomField itemField = record.getCustomField(fieldDef.getName());
        if (itemField != null && itemField.getValue() != null) {
          customFields.put(fieldDef.getName(), itemField.getValue());
        }
      }
      if (customFields.isEmpty()) {
        customFields = null;
      }
    }
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public String getName() {
    return name;
  }

  public String getSummary() {
    return summary;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public String getStreet() {
    return street;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getBarcode() {
    return barcode;
  }

  public String[] getTags() {
    return tags;
  }

  @JsonAnyGetter
  public Map<String, String> getProperties() {
    return customFields;
  }
}
