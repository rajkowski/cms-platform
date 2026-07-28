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

package com.zeroio.platform.application.cms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.zeroio.platform.domain.model.cms.WebPageVersion;
import com.zeroio.platform.infrastructure.persistence.cms.WebPageVersionRepository;

/**
 * Restores a web page to a previously saved XML version
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SetCurrentWebPageVersionCommand {

  public static WebPage setCurrentVersion(long webPageId, long versionId, long userId) throws DataException {

    if (webPageId <= 0) {
      throw new DataException("A web page id is required");
    }
    if (versionId <= 0) {
      throw new DataException("A version id is required");
    }

    WebPage webPage = WebPageRepository.findById(webPageId);
    if (webPage == null) {
      throw new DataException("The web page could not be found");
    }

    WebPageVersion version = WebPageVersionRepository.findById(versionId);
    if (version == null) {
      throw new DataException("The version could not be found");
    }
    if (version.getWebPageId() != webPageId) {
      throw new DataException("The version does not belong to this web page");
    }
    if (StringUtils.isBlank(version.getPageXml())) {
      throw new DataException("The selected version has no XML to restore");
    }

    if (StringUtils.isNotBlank(webPage.getPageXml()) && !Strings.CS.equals(webPage.getPageXml(), version.getPageXml())) {
      WebPageVersion beforeRestore = new WebPageVersion();
      beforeRestore.setWebPageId(webPage.getId());
      beforeRestore.setPageXml(webPage.getPageXml());
      beforeRestore.setCreatedBy(userId);
      beforeRestore.setNotes("Version saved before restore");
      WebPageVersionRepository.save(beforeRestore);
    }

    webPage.setPageXml(version.getPageXml());
    webPage.setModifiedBy(userId);
    return WebPageRepository.save(webPage);
  }
}