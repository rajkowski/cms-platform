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

package com.zeroio.platform.application.items;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simisinc.platform.application.CustomFieldListJSONCommand;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Item;

/**
 * Builds an Item object from a JSON version string
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class BuildItemFromVersionCommand {

  public static Item parseVersion(Item currentItem, String versionData, long userId) throws DataException {
    JsonNode root;
    try {
      root = new ObjectMapper().readTree(versionData);
    } catch (Exception e) {
      throw new DataException("The selected version data is invalid");
    }

    Item itemBean = new Item();
    itemBean.setId(currentItem.getId());
    itemBean.setCollectionId(readLong(root, "collection_id", currentItem.getCollectionId()));
    itemBean.setCreatedBy(readLong(root, "created_by", currentItem.getCreatedBy()));
    itemBean.setModifiedBy(userId);

    itemBean.setUniqueId(readText(root, "unique_id", currentItem.getUniqueId()));
    itemBean.setName(readText(root, "name", currentItem.getName()));
    itemBean.setSummary(readText(root, "summary", currentItem.getSummary()));
    itemBean.setDescription(readText(root, "description", currentItem.getDescription()));
    itemBean.setCategoryId(readLong(root, "category_id", currentItem.getCategoryId()));
    itemBean.setCategoryIdList(readLongArray(root.get("category_id_list"), currentItem.getCategoryIdList()));

    itemBean.setLatitude(readDouble(root, "latitude", currentItem.getLatitude()));
    itemBean.setLongitude(readDouble(root, "longitude", currentItem.getLongitude()));
    itemBean.setGeoJSON(readText(root, "geojson", currentItem.getGeoJSON()));
    itemBean.setLocation(readText(root, "location_name", currentItem.getLocation()));
    itemBean.setStreet(readText(root, "street", currentItem.getStreet()));
    itemBean.setAddressLine2(readText(root, "address_line_2", currentItem.getAddressLine2()));
    itemBean.setAddressLine3(readText(root, "address_line_3", currentItem.getAddressLine3()));
    itemBean.setCity(readText(root, "city", currentItem.getCity()));
    itemBean.setState(readText(root, "state", currentItem.getState()));
    itemBean.setCountry(readText(root, "country", currentItem.getCountry()));
    itemBean.setPostalCode(readText(root, "postal_code", currentItem.getPostalCode()));
    itemBean.setCounty(readText(root, "county", currentItem.getCounty()));

    itemBean.setPhoneNumber(readText(root, "phone_number", currentItem.getPhoneNumber()));
    itemBean.setEmail(readText(root, "email", currentItem.getEmail()));
    itemBean.setCost(readBigDecimal(root, "cost", currentItem.getCost()));
    itemBean.setExpectedDate(readTimestamp(root, "expected_date", currentItem.getExpectedDate()));
    itemBean.setStartDate(readTimestamp(root, "start_date", currentItem.getStartDate()));
    itemBean.setEndDate(readTimestamp(root, "end_date", currentItem.getEndDate()));
    itemBean.setExpirationDate(readTimestamp(root, "expiration_date", currentItem.getExpirationDate()));

    itemBean.setUrl(readText(root, "url", currentItem.getUrl()));
    itemBean.setUrlText(readText(root, "url_text", currentItem.getUrlText()));
    itemBean.setImageUrl(readText(root, "image_url", currentItem.getImageUrl()));
    itemBean.setBarcode(readText(root, "barcode", currentItem.getBarcode()));
    itemBean.setKeywords(readText(root, "keywords", currentItem.getKeywords()));
    itemBean.setTags(readStringArray(root.get("tags"), currentItem.getTags()));

    itemBean.setCustomFieldList(readCustomFieldList(root.get("field_values"), currentItem.getCustomFieldList()));
    itemBean.setIpAddress(currentItem.getIpAddress());

    return itemBean;
  }

  private static long readLong(JsonNode root, String fieldName, long defaultValue) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return defaultValue;
    }
    if (node.isNumber()) {
      return node.longValue();
    }
    if (node.isTextual()) {
      try {
        return Long.parseLong(node.asText());
      } catch (NumberFormatException ignored) {
        // Ignore malformed values and keep the current field value.
      }
    }
    return defaultValue;
  }

  private static double readDouble(JsonNode root, String fieldName, double defaultValue) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return defaultValue;
    }
    if (node.isNumber()) {
      return node.doubleValue();
    }
    if (node.isTextual()) {
      try {
        return Double.parseDouble(node.asText());
      } catch (NumberFormatException ignored) {
        // Ignore malformed values and keep the current field value.
      }
    }
    return defaultValue;
  }

  private static BigDecimal readBigDecimal(JsonNode root, String fieldName, BigDecimal defaultValue) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return defaultValue;
    }
    try {
      return new BigDecimal(node.asText());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static Timestamp readTimestamp(JsonNode root, String fieldName, Timestamp defaultValue) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return defaultValue;
    }
    String value = node.asText();
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    try {
      return Timestamp.from(OffsetDateTime.parse(value).toInstant());
    } catch (DateTimeParseException ignored) {
      // Try the next timestamp format.
    }
    try {
      return Timestamp.valueOf(LocalDateTime.parse(value));
    } catch (DateTimeParseException ignored) {
      // Try the next timestamp format.
    }
    try {
      return Timestamp.from(Instant.parse(value));
    } catch (DateTimeParseException ignored) {
      // Ignore malformed values and keep the current field value.
    }
    return defaultValue;
  }

  private static String readText(JsonNode root, String fieldName, String defaultValue) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return defaultValue;
    }
    return node.asText();
  }

  private static Long[] readLongArray(JsonNode node, Long[] defaultValue) {
    if (node == null || !node.isArray()) {
      return defaultValue;
    }
    List<Long> values = new ArrayList<>();
    for (JsonNode valueNode : node) {
      if (valueNode == null || valueNode.isNull()) {
        continue;
      }
      try {
        values.add(valueNode.longValue());
      } catch (Exception ignored) {
        // Skip values that cannot be parsed as long.
      }
    }
    return values.toArray(new Long[0]);
  }

  private static String[] readStringArray(JsonNode node, String[] defaultValue) {
    if (node == null || !node.isArray()) {
      return defaultValue;
    }
    List<String> values = new ArrayList<>();
    for (JsonNode valueNode : node) {
      if (valueNode == null || valueNode.isNull()) {
        continue;
      }
      values.add(valueNode.asText());
    }
    return values.toArray(new String[0]);
  }

  private static Map<String, CustomField> readCustomFieldList(JsonNode node, Map<String, CustomField> defaultValue)
      throws DataException {
    if (node == null || !node.isArray()) {
      return defaultValue;
    }
    try {
      return CustomFieldListJSONCommand.populateFromJSONString(node.toString());
    } catch (SQLException e) {
      throw new DataException("The selected version has invalid custom field data");
    }
  }
}
