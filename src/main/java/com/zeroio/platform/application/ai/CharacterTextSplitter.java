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

import java.util.ArrayList;
import java.util.List;

public class CharacterTextSplitter {

  // Split text into sentences or '\n'; group sentences together until reaching word count; never split in the middle of a sentence (create a new chunk)

  public static final int CHARACTER_CHUNK_SIZE = 500;
  public static final double CHUNK_OVERLAP = 0.2;

  // default text separators
  public static final String PARAGRAPH_SEPARATOR = "\n\n";
  public static final String SENTENCE_SEPARATOR = "(?<=\\n)|(?<=[.!?])";

  /**
   * Recursively splits the given text into chunks of a specified character size, ensuring that no chunk exceeds the maximum character chunk size.
   * If it's too large, then reduce the chunk by keeping all paragraphs, then sentences, then words together as long as possible.
   * 
   * @param text The text to be split into chunks.
   * @return A list of text chunks, each not exceeding the maximum character chunk size.
   */
  public static List<String> recursivelySplitText(String text) {
    List<String> finalChunks = new ArrayList<>();
    if (text.length() <= CHARACTER_CHUNK_SIZE) {
      finalChunks.add(text);
      return finalChunks;
    }

    int overlapSize = (int) (CHARACTER_CHUNK_SIZE * CHUNK_OVERLAP);
    int start = 0;
    while (start < text.length()) {
      int maxEnd = Math.min(start + CHARACTER_CHUNK_SIZE, text.length());
      int end = findChunkEnd(text, start, maxEnd, overlapSize);
      finalChunks.add(text.substring(start, end));
      if (end == text.length()) {
        break;
      }
      start = findChunkStart(text, start, end, overlapSize);
    }
    return finalChunks;
  }

  private static int findChunkStart(String text, int start, int end, int overlapSize) {
    int overlapStart = end - overlapSize;
    for (int index = overlapStart - 1; index > start; index--) {
      if (Character.isWhitespace(text.charAt(index))) {
        return index + 1;
      }
    }
    return overlapStart;
  }

  private static int findChunkEnd(String text, int start, int maxEnd, int overlapSize) {
    int earliestBoundary = Math.min(start + overlapSize + 1, maxEnd);
    int sentenceBoundary = -1;
    int lineBoundary = -1;
    int wordBoundary = -1;
    for (int index = earliestBoundary; index < maxEnd; index++) {
      char character = text.charAt(index);
      if (character == '.' || character == '!' || character == '?') {
        sentenceBoundary = index + 1;
        while (sentenceBoundary < maxEnd && Character.isWhitespace(text.charAt(sentenceBoundary))) {
          sentenceBoundary++;
        }
      }
      if (character == '\n') {
        lineBoundary = index + 1;
      }
      if (Character.isWhitespace(character)) {
        wordBoundary = index + 1;
      }
    }
    if (sentenceBoundary > start) {
      return sentenceBoundary;
    }
    if (lineBoundary > start) {
      return lineBoundary;
    }
    if (wordBoundary > start) {
      return wordBoundary;
    }
    return maxEnd;
  }
}
