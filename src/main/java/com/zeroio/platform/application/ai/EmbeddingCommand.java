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
package com.zeroio.platform.application.ai;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;

/**
 * Creates embeddings using a local ONNX model
 *
 * @author matt rajkowski
 * @created 11/5/26 3:00 PM
 */
public class EmbeddingCommand {

  private static Log LOG = LogFactory.getLog(EmbeddingCommand.class);

  // Examples:
  //  ibm-granite/granite-embedding-311m-multilingual-r2 (768)
  //  ibm-granite/granite-embedding-english-r2 (768)
  //  ibm-granite/granite-embedding-small-english-r2 (384)

  public static final String MODEL_NAME = "ibm-granite/granite-embedding-small-english-r2";
  public static final int DIMENSIONS = 384;

  private static final String MODEL_PATH_ENV = "CMS_EMBEDDING_MODEL_PATH";
  private static final String DEFAULT_MODEL_PATH = "./models";

  private static volatile EmbeddingModel embeddingModel = null;

  /** The model.onnx and tokenizer.json must be in the model folder */
  public static boolean isAvailable() {
    File modelPath = getModelPath();
    return new File(modelPath, "model.onnx").isFile() && new File(modelPath, "tokenizer.json").isFile();
  }

  public static List<float[]> embed(List<String> texts) {
    List<TextSegment> segments = new ArrayList<>(texts.size());
    for (String text : texts) {
      segments.add(TextSegment.from(text));
    }
    List<float[]> vectors = new ArrayList<>(texts.size());
    for (Embedding embedding : getModel().embedAll(segments).content()) {
      vectors.add(embedding.vector());
    }
    return vectors;
  }

  private static File getModelPath() {
    String configuredPath = System.getenv(MODEL_PATH_ENV);
    if (StringUtils.isNotBlank(configuredPath)) {
      return new File(configuredPath);
    }
    return new File(DEFAULT_MODEL_PATH);
  }

  private static EmbeddingModel getModel() {
    if (embeddingModel == null) {
      synchronized (EmbeddingCommand.class) {
        if (embeddingModel == null) {
          File modelPath = getModelPath();
          LOG.info("Loading embedding model from " + modelPath.getPath());
          embeddingModel = new OnnxEmbeddingModel(new File(modelPath, "model.onnx").getPath(),
              new File(modelPath, "tokenizer.json").getPath(), PoolingMode.MEAN);
        }
      }
    }
    return embeddingModel;
  }
}
