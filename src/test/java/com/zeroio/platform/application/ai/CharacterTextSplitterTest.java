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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CharacterTextSplitterTest {

  @Test
  void returnsShortTextAsASingleChunk() {
    String text = "A short paragraph.\nAnother line.";

    List<String> chunks = CharacterTextSplitter.recursivelySplitText(text);

    Assertions.assertEquals(List.of(text), chunks);
  }

  @Test
  void preservesSentenceBoundariesAndAddsConfiguredOverlap() {
    String firstSentence = "a".repeat(450) + ". ";
    String text = firstSentence + "b".repeat(220) + ".";

    List<String> chunks = CharacterTextSplitter.recursivelySplitText(text);

    Assertions.assertTrue(chunks.size() > 1);
    Assertions.assertTrue(chunks.get(0).endsWith(firstSentence));
    int overlapSize = (int) (CharacterTextSplitter.CHARACTER_CHUNK_SIZE * CharacterTextSplitter.CHUNK_OVERLAP);
    Assertions.assertTrue(chunks.get(1).startsWith(chunks.get(0).substring(chunks.get(0).length() - overlapSize)));
    assertChunksRespectSizeAndReconstructText(text, chunks);
  }

  @Test
  void hardSplitsTextWithoutNaturalBoundaries() {
    String text = "x".repeat(CharacterTextSplitter.CHARACTER_CHUNK_SIZE * 3);

    List<String> chunks = CharacterTextSplitter.recursivelySplitText(text);

    Assertions.assertEquals(4, chunks.size());
    assertChunksRespectSizeAndReconstructText(text, chunks);
  }

  @Test
  void keepsWordsWholeAcrossChunkBoundaries() {
    StringBuilder textBuilder = new StringBuilder();
    for (int index = 0; index < 200; index++) {
      textBuilder.append("word").append(index).append(' ');
    }
    String text = textBuilder.toString();
    List<String> words = List.of(text.trim().split(" "));

    List<String> chunks = CharacterTextSplitter.recursivelySplitText(text);

    Assertions.assertTrue(chunks.size() > 1);
    for (String chunk : chunks) {
      for (String word : chunk.trim().split(" ")) {
        Assertions.assertTrue(words.contains(word), "Chunk contains partial word: " + word);
      }
    }
  }

  private void assertChunksRespectSizeAndReconstructText(String text, List<String> chunks) {
    StringBuilder reconstructedText = new StringBuilder(chunks.get(0));
    for (int index = 0; index < chunks.size(); index++) {
      String chunk = chunks.get(index);
      Assertions.assertTrue(chunk.length() <= CharacterTextSplitter.CHARACTER_CHUNK_SIZE);
      if (index > 0) {
        int overlapSize = (int) (CharacterTextSplitter.CHARACTER_CHUNK_SIZE * CharacterTextSplitter.CHUNK_OVERLAP);
        Assertions.assertTrue(chunks.get(index - 1).endsWith(chunk.substring(0, overlapSize)));
        reconstructedText.append(chunk.substring(overlapSize));
      }
    }
    Assertions.assertEquals(text, reconstructedText.toString());
  }
}