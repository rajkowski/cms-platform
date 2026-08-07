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

package com.zeroio.platform.infrastructure.scheduler.cms;

import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;

import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.distributedlock.LockManager;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileSpecification;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileSpecification;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;
import com.zeroio.platform.application.cms.FileItemDocumentContentCommand;
import com.zeroio.platform.application.cms.ItemFileItemDocumentContentCommand;

import lombok.NoArgsConstructor;

/**
 * Async job for updating all document text indexes.
 *
 * @author matt rajkowski
 * @created 8/6/26 5:00 PM
 */
@NoArgsConstructor
public class RefreshAllDocumentTextIndexesJob {

  private static Log LOG = LogFactory.getLog(RefreshAllDocumentTextIndexesJob.class);

  @Job(name = "Refresh all document text indexes for content")
  public static void execute() {
    // Distributed lock
    String lock = LockManager.lock(SchedulerManager.REFRESH_ALL_DOCUMENT_TEXT_INDEXES_JOB, Duration.ofMinutes(30));
    if (lock == null) {
      return;
    }

    try {
      // Page through all files and update text indexes
      FileSpecification fileSpecification = new FileSpecification();
      fileSpecification.setIsProcessed(false);
      fileSpecification.setFileType("pdf");
      List<FileItem> allFiles = FileItemRepository.findAll(fileSpecification, null);
      allFiles.forEach(fileItem -> FileItemDocumentContentCommand.updateTextIndex(fileItem));

      // Page through all item files and update text indexes
      ItemFileSpecification itemFileSpecification = new ItemFileSpecification();
      itemFileSpecification.setIsProcessed(false);
      itemFileSpecification.setFileType("pdf");
      List<ItemFileItem> allItemFiles = ItemFileItemRepository.findAll(itemFileSpecification, null);
      allItemFiles.forEach(itemFileItem -> ItemFileItemDocumentContentCommand.updateTextIndex(itemFileItem));
    } catch (Exception e) {
      LOG.warn("Error refreshing all document text indexes: " + e.getMessage(), e);
    } finally {
      // Finish up
      LockManager.unlock(SchedulerManager.REFRESH_ALL_DOCUMENT_TEXT_INDEXES_JOB, lock);
    }
  }
}
