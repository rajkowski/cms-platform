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

package com.simisinc.platform.application.oauth;

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.simisinc.platform.application.json.JsonCommand;

/**
 * Parses and validates JWTs
 *
 * @author matt rajkowski
 * @created 4/9/24 9:19 PM
 */
public class JWTCommand {

  private static Log LOG = LogFactory.getLog(JWTCommand.class);

  public static JsonNode parseJwt(String jwt) {
    if (StringUtils.isBlank(jwt) || !jwt.contains(".")) {
      LOG.debug("String does not appear to be a JWT");
      return null;
    }

    // JWT
    String[] chunks = jwt.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();

    // Extract JWT details
    String header = new String(decoder.decode(chunks[0]));
    String payload = new String(decoder.decode(chunks[1]));
    String signature = chunks[2];
    if (StringUtils.isAnyBlank(header, payload, signature)) {
      LOG.debug("String does not have JWT chunks");
      return null;
    }

    // Return the payload
    try {
      return JsonCommand.fromString(payload);
    } catch (Exception e) {
      LOG.debug("JsonLoad exception", e);
      return null;
    }
  }
}
