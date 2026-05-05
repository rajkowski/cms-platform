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

package com.simisinc.platform.application.datasets;

import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author matt rajkowski
 * @created 4/28/2026 12:00 PM
 */
class SaveDatasetRowCommandTest {

  @Test
  void constructItemMapsCommaDelimitedTags() {
    Item item = new Item();
    Dataset dataset = new Dataset();
    Collection collection = new Collection();
    collection.setId(1L);

    SaveDatasetRowCommand.constructItem(
        item,
        new String[] { "Tag A, Tag B,Tag A" },
        dataset,
        collection,
        Collections.singletonList("tags_column"),
        Collections.singletonList("tags_column"),
        Collections.singletonList("tags"),
        Collections.singletonList(""));

    Assertions.assertArrayEquals(new String[] { "Tag A", "Tag B" }, item.getTags());
  }

  @Test
  void constructItemMapsSemicolonDelimitedTags() {
    Item item = new Item();
    Dataset dataset = new Dataset();
    Collection collection = new Collection();
    collection.setId(1L);

    SaveDatasetRowCommand.constructItem(
        item,
        new String[] { "Tag A; Tag B ; ;Tag C" },
        dataset,
        collection,
        Collections.singletonList("tags_column"),
        Collections.singletonList("tags_column"),
        Collections.singletonList("tags"),
        Collections.singletonList(""));

    Assertions.assertArrayEquals(new String[] { "Tag A", "Tag B", "Tag C" }, item.getTags());
  }

  @Test
  void constructItemMapsTagsWithSplitOption() {
    Item item = new Item();
    Dataset dataset = new Dataset();
    Collection collection = new Collection();
    collection.setId(1L);

    SaveDatasetRowCommand.constructItem(
        item,
        new String[] { "Tag A|Tag B|Tag C" },
        dataset,
        collection,
        Collections.singletonList("tags_column"),
        Collections.singletonList("tags_column"),
        Collections.singletonList("tags"),
        Arrays.asList("split(\"|\")"));

    Assertions.assertArrayEquals(new String[] { "Tag A", "Tag B", "Tag C" }, item.getTags());
  }
}
