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

package com.simisinc.platform.application;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.domain.model.App;

/**
 * @author matt rajkowski
 * @created 5/5/2026 12:00 PM
 */
class RateLimitCommandTest {

  @Test
  void matchesLenientAppById() {
    App app = new App();
    app.setId(42L);
    app.setPublicKey("public-key-1");

    boolean isLenient = RateLimitCommand.isLenientRateLimitApp(app, Arrays.asList("41", "42", "43"));

    Assertions.assertTrue(isLenient);
  }

  @Test
  void matchesLenientAppByPublicKey() {
    App app = new App();
    app.setId(42L);
    app.setPublicKey("public-key-1");

    boolean isLenient = RateLimitCommand.isLenientRateLimitApp(app, Arrays.asList("10", "public-key-1"));

    Assertions.assertTrue(isLenient);
  }

  @Test
  void returnsFalseWhenNotConfigured() {
    App app = new App();
    app.setId(42L);
    app.setPublicKey("public-key-1");

    boolean isLenient = RateLimitCommand.isLenientRateLimitApp(app, Collections.singletonList("99"));

    Assertions.assertFalse(isLenient);
  }
}
