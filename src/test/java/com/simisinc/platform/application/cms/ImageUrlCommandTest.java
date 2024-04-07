/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

class ImageUrlCommandTest {
  @Test
  void testExtractFileId() {
    String uri = "20180503171549-5";
    long fileId = ImageUrlCommand.extractFileId(uri);
    assertEquals(5L, fileId);
  }

  @Test
  void testExtractWebPath() {
    String uri = "20180503171549-5";
    String webPath = ImageUrlCommand.extractWebPath(uri);
    assertEquals("20180503171549", webPath);

  }

  @Test
  void testReduceImageUri() {
    String uri = "/20180503171549-5/logo.png";
    String resourceValue = ImageUrlCommand.reduceImageUri(uri);
    assertNotNull(resourceValue);
    assertEquals("20180503171549-5", resourceValue);
  }

  @Test
  void testReduceImageAssetUri() {
    String uri = "/asset/img/20180503171549-5/logo.png";
    String resourceValue = ImageUrlCommand.reduceImageUri(uri);
    assertNotNull(resourceValue);
    assertEquals("20180503171549-5", resourceValue);
  }
}
