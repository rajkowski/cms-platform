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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.GitPublishSettings;
import com.simisinc.platform.infrastructure.persistence.cms.GitPublishSettingsRepository;

/**
 * Saves Git publish settings to the database
 *
 * @author matt rajkowski
 * @created 2/14/26 2:15 PM
 */
public class SaveGitPublishSettingsCommand {

  private static Log LOG = LogFactory.getLog(SaveGitPublishSettingsCommand.class);

  public static GitPublishSettings saveSettings(GitPublishSettings settingsBean) throws DataException {

    // Required fields
    if (settingsBean.getEnabled()) {
      if (StringUtils.isBlank(settingsBean.getGitProvider())) {
        throw new DataException("Git provider is required");
      }
      if (StringUtils.isBlank(settingsBean.getRepositoryUrl())) {
        throw new DataException("Repository URL is required");
      }
      if (StringUtils.isBlank(settingsBean.getBranchName())) {
        throw new DataException("Branch name is required");
      }
      if (StringUtils.isBlank(settingsBean.getAccessToken())) {
        throw new DataException("Access token is required");
      }
      if (StringUtils.isBlank(settingsBean.getUsername())) {
        throw new DataException("Username is required");
      }
      if (StringUtils.isBlank(settingsBean.getEmail())) {
        throw new DataException("Email is required");
      }
    }

    // Save the settings
    return GitPublishSettingsRepository.save(settingsBean);
  }
}
