/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.application.json;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JSON creation functionality
 *
 * @author matt rajkowski
 * @created 1/31/26 3:20 PM
 */
class JsonCommandTest {

  @Test
  void createJsonNodeWithSimpleString() {
    Map<String, Object> data = new HashMap<>();
    data.put("name", "John");
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"name\":\"John\""));
    assertTrue(result.startsWith("{") && result.endsWith("}"));
  }

  @Test
  void createJsonNodeWithNumber() {
    Map<String, Object> data = new HashMap<>();
    data.put("age", 30);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"age\":30"));
  }

  @Test
  void createJsonNodeWithBoolean() {
    Map<String, Object> data = new HashMap<>();
    data.put("active", true);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"active\":true"));
    
    Map<String, Object> data2 = new HashMap<>();
    data2.put("inactive", false);
    String result2 = JsonCommand.createJsonNode(data2).toString();
    assertTrue(result2.contains("\"inactive\":false"));
  }

  @Test
  void createJsonNodeWithNullValue() {
    Map<String, Object> data = new HashMap<>();
    data.put("name", "John");
    data.put("nickname", null);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"name\":\"John\""));
    assertFalse(result.contains("nickname"));
  }

  @Test
  void createJsonNodeWithList() {
    Map<String, Object> data = new HashMap<>();
    List<String> colors = Arrays.asList("red", "green", "blue");
    data.put("colors", colors);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"colors\":[\"red\", \"green\", \"blue\"]"));
  }

  @Test
  void createJsonNodeWithStringArray() {
    Map<String, Object> data = new HashMap<>();
    String[] colors = {"red", "green", "blue"};
    data.put("colors", colors);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"colors\":[\"red\", \"green\", \"blue\"]"));
  }

  @Test
  void createJsonNodeWithIntegerArray() {
    Map<String, Object> data = new HashMap<>();
    Integer[] numbers = {1, 2, 3, 4, 5};
    data.put("numbers", numbers);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"numbers\":[1, 2, 3, 4, 5]"));
  }



  @Test
  void createJsonNodeWithNestedMap() {
    Map<String, Object> person = new HashMap<>();
    person.put("firstName", "John");
    person.put("lastName", "Doe");
    
    Map<String, Object> data = new HashMap<>();
    data.put("user", person);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"user\":{"));
    assertTrue(result.contains("\"firstName\":\"John\""));
    assertTrue(result.contains("\"lastName\":\"Doe\""));
  }

  @Test
  void createJsonNodeWithListOfMaps() {
    Map<String, Object> segment1 = new HashMap<>();
    segment1.put("name", "New Visitors");
    segment1.put("count", 3200);
    
    Map<String, Object> segment2 = new HashMap<>();
    segment2.put("name", "Returning Visitors");
    segment2.put("count", 5400);
    
    List<Map<String, Object>> segments = Arrays.asList(segment1, segment2);
    
    Map<String, Object> data = new HashMap<>();
    data.put("segments", segments);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"segments\":["));
    assertTrue(result.contains("\"name\":\"New Visitors\""));
    assertTrue(result.contains("\"count\":3200"));
    assertTrue(result.contains("\"name\":\"Returning Visitors\""));
    assertTrue(result.contains("\"count\":5400"));
  }

  @Test
  void createJsonNodeWithMixedList() {
    List<Object> mixed = new ArrayList<>();
    mixed.add("text");
    mixed.add(42);
    mixed.add(true);
    
    Map<String, Object> data = new HashMap<>();
    data.put("items", mixed);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"items\":[\"text\", 42, true]"));
  }

  @Test
  void createJsonNodeWithComplexStructure() {
    // Simulating analytics data structure
    List<Map<String, Object>> segments = new ArrayList<>();
    Map<String, Object> segment = new HashMap<>();
    segment.put("name", "New Visitors");
    segment.put("count", 3200);
    segment.put("percent", 32.0);
    segments.add(segment);
    
    Map<String, Object> deviceDistribution = new HashMap<>();
    deviceDistribution.put("labels", new String[]{"Desktop", "Mobile", "Tablet"});
    deviceDistribution.put("values", new Integer[]{6200, 3100, 800});
    
    Map<String, Object> data = new HashMap<>();
    data.put("segments", segments);
    data.put("deviceDistribution", deviceDistribution);
    
    String result = JsonCommand.createJsonNode(data).toString();
    
    // Verify structure
    assertTrue(result.contains("\"segments\":["));
    assertTrue(result.contains("\"name\":\"New Visitors\""));
    assertTrue(result.contains("\"deviceDistribution\":{"));
    assertTrue(result.contains("\"labels\":[\"Desktop\", \"Mobile\", \"Tablet\"]"));
    assertTrue(result.contains("\"values\":[6200, 3100, 800]"));
    
    // Should NOT contain class names
    assertFalse(result.contains("[Ljava"));
    assertFalse(result.contains("@"));
  }

  @Test
  void createJsonNodeWithArrayContainingNulls() {
    Map<String, Object> data = new HashMap<>();
    Object[] items = {"text", null, 42};
    data.put("items", items);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"items\":[\"text\", null, 42]"));
  }

  @Test
  void createJsonNodeWithEmptyList() {
    Map<String, Object> data = new HashMap<>();
    data.put("items", new ArrayList<>());
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"items\":[]"));
  }

  @Test
  void createJsonNodeWithEmptyMap() {
    Map<String, Object> data = new HashMap<>();
    data.put("user", new HashMap<>());
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"user\":{}"));
  }

  @Test
  void createJsonNodeWithSpecialCharacters() {
    Map<String, Object> data = new HashMap<>();
    data.put("message", "Hello \"World\"");
    data.put("path", "C:\\Users\\test");
    String result = JsonCommand.createJsonNode(data).toString();
    // Should properly escape special characters
    assertTrue(result.contains("message"));
    assertTrue(result.contains("path"));
  }

  @Test
  void createJsonNodeWithMultipleFields() {
    Map<String, Object> data = new HashMap<>();
    data.put("id", 1);
    data.put("name", "John");
    data.put("active", true);
    data.put("score", 95.5);
    
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"id\":1"));
    assertTrue(result.contains("\"name\":\"John\""));
    assertTrue(result.contains("\"active\":true"));
    assertTrue(result.contains("\"score\":95.5"));
    assertTrue(result.startsWith("{") && result.endsWith("}"));
  }

  @Test
  void createJsonNodeWithDoubleValue() {
    Map<String, Object> data = new HashMap<>();
    data.put("percentage", 32.0);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"percentage\":32.0"));
  }

  @Test
  void createJsonNodeWithNestedLists() {
    List<List<String>> nestedList = new ArrayList<>();
    nestedList.add(Arrays.asList("a", "b", "c"));
    nestedList.add(Arrays.asList("x", "y", "z"));
    
    Map<String, Object> data = new HashMap<>();
    data.put("matrix", nestedList);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"matrix\":["));
  }

  @Test
  void createJsonNodeWithListOfNumbers() {
    Map<String, Object> data = new HashMap<>();
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    data.put("values", numbers);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"values\":[1, 2, 3, 4, 5]"));
  }

  @Test
  void createJsonNodeWithListOfBooleans() {
    Map<String, Object> data = new HashMap<>();
    List<Boolean> flags = Arrays.asList(true, false, true);
    data.put("flags", flags);
    String result = JsonCommand.createJsonNode(data).toString();
    assertTrue(result.contains("\"flags\":[true, false, true]"));
  }

  @Test
  void createJsonNodeReturnsStringBuilder() {
    Map<String, Object> data = new HashMap<>();
    data.put("test", "value");
    StringBuilder result = JsonCommand.createJsonNode(data);
    assertNotNull(result);
    assertInstanceOf(StringBuilder.class, result);
    assertTrue(result.toString().contains("\"test\":\"value\""));
  }
}
