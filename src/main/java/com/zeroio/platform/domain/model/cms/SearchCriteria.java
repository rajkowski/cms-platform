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
package com.zeroio.platform.domain.model.cms;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.model.Entity;

/**
 * A search criteria to be used for searching
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SearchCriteria extends Entity {

  static final long serialVersionUID = -8484048371911908893L;

  public static final String ALL = "all";
  public static final String PAGES = "pages";
  public static final String CALENDAR = "calendar";
  public static final String ATTACHMENTS = "attachments";
  public static final String POSTS = "posts";
  public static final String RESOURCES = "resources";

  private String query = null;
  private String[] tags = null;
  private String ofType = null;
  private String dateFilterType = null;
  private Timestamp fromDate = null;
  private String fromDateValue = null;
  private Timestamp toDate = null;
  private String toDateValue = null;
  private long[] contributorFilter = null;

  public SearchCriteria() {
  }

  public SearchCriteria(Map<String, String[]> parameterMap) {
    // Extract parameters from the map
    this.query = getFirstParameter(parameterMap, "query");
    this.ofType = getFirstParameter(parameterMap, "ofType");
    this.dateFilterType = getFirstParameter(parameterMap, "dateFilterType");

    String fromDateParam = getFirstParameter(parameterMap, "fromDate");
    if (fromDateParam == null) {
      // Fallback to modifiedAfter if fromDate is not provided
      fromDateParam = getFirstParameter(parameterMap, "modifiedAfter");
    }
    this.fromDateValue = fromDateParam;
    this.fromDate = parseAfterTimestamp(fromDateParam);

    String toDateParam = getFirstParameter(parameterMap, "toDate");
    if (toDateParam == null) {
      // Fallback to modifiedBefore if toDate is not provided
      toDateParam = getFirstParameter(parameterMap, "modifiedBefore");
    }
    this.toDateValue = toDateParam;
    this.toDate = parseBeforeTimestamp(toDateParam);

    this.contributorFilter = parseContributorFilter(getFirstParameter(parameterMap, "contributorFilter"));

    String tagsParam = getFirstParameter(parameterMap, "tags");
    if (tagsParam == null) {
      // Fallback to filterTags if tags is not provided
      tagsParam = getFirstParameter(parameterMap, "label");
    }
    this.tags = parseTags(tagsParam);
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public boolean hasTags() {
    return tags != null && tags.length > 0;
  }

  public String getOfType() {
    return ofType;
  }

  public void setOfType(String ofType) {
    this.ofType = ofType;
  }

  public boolean hasOfType() {
    return ofType != null && !ofType.isEmpty() && !ofType.equals(ALL);
  }

  public String getDateFilterType() {
    return dateFilterType;
  }

  public void setDateFilterType(String dateFilterType) {
    this.dateFilterType = dateFilterType;
  }

  public boolean hasDateFilter() {
    return dateFilterType != null && !dateFilterType.isEmpty();
  }

  public boolean getHasDateFilter() {
    return hasDateFilter();
  }

  public Timestamp getFromDate() {
    return fromDate;
  }

  public String getFromDateValue() {
    return fromDateValue;
  }

  public void setFromDate(Timestamp fromDate) {
    this.fromDate = fromDate;
  }

  public Timestamp getToDate() {
    return toDate;
  }

  public String getToDateValue() {
    return toDateValue;
  }

  public void setToDate(Timestamp toDate) {
    this.toDate = toDate;
  }

  public long[] getContributorFilter() {
    return contributorFilter;
  }

  public void setContributorFilter(long[] contributorFilter) {
    this.contributorFilter = contributorFilter;
  }

  public boolean hasContributorFilter() {
    return contributorFilter != null && contributorFilter.length > 0;
  }

  public boolean hasFilters() {
    return (query != null && !query.isEmpty()) ||
        hasTags() ||
        hasOfType() ||
        hasDateFilter() ||
        fromDate != null ||
        toDate != null ||
        hasContributorFilter();
  }

  public boolean getHasFilters() {
    return hasFilters();
  }

  public static String getFirstParameter(Map<String, String[]> parameterMap, String key) {
    String[] values = parameterMap.get(key);
    if (values != null && values.length > 0) {
      return values[0].trim();
    }
    return null;
  }

  public static long[] parseContributorFilter(String value) {
    if (value != null && !value.isEmpty()) {
      try {
        String[] userIdStrings = value.split(",");
        long[] userIds = new long[userIdStrings.length];
        for (int i = 0; i < userIdStrings.length; i++) {
          userIds[i] = Long.parseLong(userIdStrings[i].trim());
        }
        return userIds;
      } catch (NumberFormatException e) {
        // Log the error and return null
        System.err.println("Error parsing contributor filter: " + value + " - " + e.getMessage());
      }
    }
    return null;
  }

  public static String[] parseTags(String value) {
    if (value != null && !value.isEmpty()) {
      String[] tagsArray = value.split(",");
      for (int i = 0; i < tagsArray.length; i++) {
        tagsArray[i] = tagsArray[i].trim();
      }
      return tagsArray;
    }
    return null;
  }

  public static Timestamp parseAfterTimestamp(String value) {
    if (value != null && !value.isEmpty()) {
      try {
        return Timestamp.valueOf(value + " 00:00:00");
      } catch (Exception e) {
        // Log the error and return null
        System.err.println("Error parsing timestamp: " + value + " - " + e.getMessage());
      }
    }
    return null;
  }

  public static Timestamp parseBeforeTimestamp(String value) {
    if (value != null && !value.isEmpty()) {
      try {
        return Timestamp.valueOf(value + " 23:59:59");
      } catch (Exception e) {
        // Log the error and return null
        System.err.println("Error parsing timestamp: " + value + " - " + e.getMessage());
      }
    }
    return null;
  }

  public String getUri() {
    StringBuilder uriBuilder = new StringBuilder("");
    if (query != null && !query.isEmpty()) {
      appendParameter(uriBuilder, "query", query);
    }
    if (tags != null && tags.length > 0) {
      // Use "label" as the parameter name for tags to maintain compatibility with existing code, but change to tags
      appendParameter(uriBuilder, "label", String.join(",", tags));
    }
    if (ofType != null && !ofType.isEmpty()) {
      appendParameter(uriBuilder, "ofType", ofType);
    }
    if (dateFilterType != null && !dateFilterType.isEmpty()) {
      appendParameter(uriBuilder, "dateFilterType", dateFilterType);
    }
    // Use "modifiedAfter" and "modifiedBefore" as the parameter names for fromDate and toDate to maintain compatibility with existing code, but change to fromDate and toDate
    if (fromDateValue != null && !fromDateValue.isEmpty()) {
      appendParameter(uriBuilder, "modifiedAfter", fromDateValue);
    }
    if (toDateValue != null && !toDateValue.isEmpty()) {
      appendParameter(uriBuilder, "modifiedBefore", toDateValue);
    }
    if (contributorFilter != null && contributorFilter.length > 0) {
      appendParameter(uriBuilder, "contributorFilter", String.join(",",
          Arrays.stream(contributorFilter).mapToObj(String::valueOf).toArray(String[]::new)));
    }
    if (uriBuilder.isEmpty()) {
      return "";
    }
    // Remove the leading "?" if present
    return uriBuilder.toString().substring(1);
  }

  public static String appendParameter(StringBuilder uri, String key, String value) {
    if (uri.isEmpty()) {
      uri.append("?").append(key).append("=").append(UrlCommand.encodeUri(value));
    } else {
      uri.append("&").append(key).append("=").append(UrlCommand.encodeUri(value));
    }
    return uri.toString();

  }

  public String toString() {
    return "SearchCriteria{" +
        "query='" + query + '\'' +
        ", tags=" + (tags != null ? String.join(", ", tags) : "null") +
        ", ofType='" + ofType + '\'' +
        ", dateFilterType='" + dateFilterType + '\'' +
        ", fromDate=" + fromDate +
        ", toDate=" + toDate +
        ", contributorFilter=" + (contributorFilter != null ? java.util.Arrays.toString(contributorFilter) : "null") +
        '}';
  }
}
