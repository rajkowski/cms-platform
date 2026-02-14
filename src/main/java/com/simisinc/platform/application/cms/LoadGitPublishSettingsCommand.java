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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.GitPublishSettings;
import com.simisinc.platform.infrastructure.persistence.cms.GitPublishSettingsRepository;

/**
 * Loads Git publish settings from the database
 *
 * @author matt rajkowski
 * @created 2/14/26 2:15 PM
 */
public class LoadGitPublishSettingsCommand {

  private static Log LOG = LogFactory.getLog(LoadGitPublishSettingsCommand.class);

  public static GitPublishSettings loadSettings() {
    return GitPublishSettingsRepository.findSettings();
  }
}
