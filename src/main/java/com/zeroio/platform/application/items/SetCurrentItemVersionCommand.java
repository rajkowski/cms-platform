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

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.items.SaveItemCommand;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.zeroio.platform.domain.model.items.ItemVersion;
import com.zeroio.platform.infrastructure.persistence.items.ItemVersionRepository;

/**
 * Restores an item to a previously saved JSON version snapshot.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SetCurrentItemVersionCommand {

  private SetCurrentItemVersionCommand() {
  }

  public static Item setCurrentVersion(long itemId, long versionId, long userId) throws DataException {

    if (itemId <= 0) {
      throw new DataException("An item id is required");
    }
    if (versionId <= 0) {
      throw new DataException("A version id is required");
    }

    Item currentItem = ItemRepository.findById(itemId);
    if (currentItem == null) {
      throw new DataException("The item could not be found");
    }

    ItemVersion version = ItemVersionRepository.findById(versionId);
    if (version == null) {
      throw new DataException("The selected version could not be found");
    }
    if (version.getItemId() != itemId) {
      throw new DataException("The selected version does not belong to this item");
    }
    if (StringUtils.isBlank(version.getVersionData())) {
      throw new DataException("The selected version has no data to restore");
    }

    // Restore the item from the version data
    Item itemBean = BuildItemFromVersionCommand.parseVersion(currentItem, version.getVersionData(), userId);
    Item restoredItem = SaveItemCommand.saveItem(itemBean);
    if (restoredItem == null) {
      throw new DataException("The item could not be restored");
    }

    return restoredItem;
  }
}