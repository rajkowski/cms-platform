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

package com.simisinc.platform.application.cms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author matt rajkowski
 * @created 4/28/2026 11:40 AM
 */
class WebPageXmlCommandTest {

  @Test
  void widgetNodeToXmlPreservesIntegerValuesWithoutDecimalSuffix() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode widgetNode = mapper.readTree("""
        {
          "type": "itemsList",
          "properties": {
            "limit": 20
          }
        }
        """);

    String xml = WebPageXmlCommand.widgetNodeToXml(widgetNode, "  ");

    Assertions.assertTrue(xml.contains("<limit>20</limit>"));
    Assertions.assertFalse(xml.contains("<limit>20.0</limit>"));
  }

  @Test
  void widgetNodeToXmlKeepsDecimalValuesWhenProvided() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode widgetNode = mapper.readTree("""
        {
          "type": "map",
          "properties": {
            "mapZoomLevel": 12.5
          }
        }
        """);

    String xml = WebPageXmlCommand.widgetNodeToXml(widgetNode, "  ");

    Assertions.assertTrue(xml.contains("<mapZoomLevel>12.5</mapZoomLevel>"));
  }
}
