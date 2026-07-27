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

package com.zeroio.platform.application.items;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.items.Item;
import com.zeroio.platform.domain.model.items.ItemVersion;
import com.zeroio.platform.infrastructure.persistence.items.ItemVersionRepository;

/**
 * Saves a version snapshot of an item before the item is updated
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SaveItemVersionCommand {

  private static Log LOG = LogFactory.getLog(SaveItemVersionCommand.class);

  public static ItemVersion saveVersion(Item item) {
    if (item == null || item.getId() == null || item.getId() == -1L) {
      return null;
    }
    ItemVersion version = ItemVersionRepository.saveVersion(item);
    if (version == null) {
      LOG.warn("Could not save item version for item_id=" + item.getId());
    } else {
      LOG.debug("Saved item version_id=" + version.getId() + " for item_id=" + item.getId());
    }
    return version;
  }
}
