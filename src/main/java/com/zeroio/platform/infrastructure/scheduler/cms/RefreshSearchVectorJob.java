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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;
import org.jsoup.Jsoup;

import com.simisinc.platform.infrastructure.distributedlock.LockManager;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;
import com.zeroio.platform.application.ai.CharacterTextSplitter;
import com.zeroio.platform.application.ai.EmbeddingCommand;
import com.zeroio.platform.infrastructure.persistence.cms.SearchChunkRepository;
import com.zeroio.platform.infrastructure.persistence.cms.SearchChunkRepository.PendingChunk;
import com.zeroio.platform.infrastructure.persistence.cms.SearchChunkRepository.Source;
import com.zeroio.platform.infrastructure.scheduler.TenantAwareJobRunner;

import lombok.NoArgsConstructor;

/**
 * Splits searchable content into chunks and creates embeddings for them.
 *
 * @author matt rajkowski
 * @created 11/5/26 3:00 PM
 */
@NoArgsConstructor
public class RefreshSearchVectorJob {

  private static Log LOG = LogFactory.getLog(RefreshSearchVectorJob.class);

  private static final int SOURCE_BATCH_SIZE = 100;
  private static final int EMBEDDING_BATCH_SIZE = 32;
  private static final int MAX_EMBEDDING_BATCHES = 200;

  // Source types whose text is HTML
  private static final List<String> HTML_TYPES = List.of("pages", "posts", "calendar", "resources");

  @Job(name = "Refresh search vector chunks and embeddings")
  public static void execute() {
    if (!EmbeddingCommand.isAvailable()) {
      LOG.debug("Embedding model files were not found, skipping");
      return;
    }
    TenantAwareJobRunner.runDefaultAndAllActive(RefreshSearchVectorJob::executeForTenant);
  }

  private static void executeForTenant() {
    // Distributed lock
    String lock = LockManager.lock(SchedulerManager.REFRESH_SEARCH_VECTORS_JOB, Duration.ofMinutes(30));
    if (lock == null) {
      return;
    }

    try {
      for (String resultType : SearchChunkRepository.SOURCE_COLUMNS.keySet()) {
        chunkSources(resultType);
      }
      embedPendingChunks();
    } catch (Exception e) {
      LOG.warn("Error refreshing search vectors: " + e.getMessage(), e);
    } finally {
      LockManager.unlock(SchedulerManager.REFRESH_SEARCH_VECTORS_JOB, lock);
    }
  }

  private static void chunkSources(String resultType) throws Exception {
    List<Source> sources = SearchChunkRepository.findSourcesToChunk(resultType, SOURCE_BATCH_SIZE);
    for (Source source : sources) {
      String text = HTML_TYPES.contains(resultType) ? Jsoup.parse(source.text()).text() : source.text();
      List<String> chunks = new ArrayList<>();
      List<String> hashes = new ArrayList<>();
      if (!text.isBlank()) {
        for (String chunk : CharacterTextSplitter.recursivelySplitText(text.trim())) {
          chunks.add(chunk);
          hashes.add(DigestUtils.sha256Hex(chunk));
        }
      }
      SearchChunkRepository.replaceChunks(resultType, source.id(), chunks, hashes);
    }
    if (!sources.isEmpty()) {
      LOG.debug("Chunked " + sources.size() + " " + resultType);
    }
  }

  private static void embedPendingChunks() throws Exception {
    for (int batch = 0; batch < MAX_EMBEDDING_BATCHES; batch++) {
      List<PendingChunk> chunks = SearchChunkRepository.findPendingEmbeddings(EMBEDDING_BATCH_SIZE);
      if (chunks.isEmpty()) {
        return;
      }
      List<String> texts = new ArrayList<>(chunks.size());
      for (PendingChunk chunk : chunks) {
        texts.add(chunk.text());
      }
      SearchChunkRepository.saveEmbeddings(chunks, EmbeddingCommand.embed(texts), EmbeddingCommand.MODEL_NAME);
    }
  }
}
