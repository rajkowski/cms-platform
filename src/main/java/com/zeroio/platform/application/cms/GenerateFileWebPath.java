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
package com.zeroio.platform.application.cms;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generates a near unique web path based on this instant of time, ensuring that the path is user-friendly for part of URLs
 * 
 * @author matt rajkowski
 * @created 8/01/2026 8:00 AM
 */
public class GenerateFileWebPath {

  public static String getWebPath() {

    // Generate a timestamp-based unique identifier
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

    // Generate a guid (letters and numbers) to ensure uniqueness
    String basePath = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    return timestamp + basePath;
  }

}
