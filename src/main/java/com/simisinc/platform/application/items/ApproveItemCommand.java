/*
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.application.items;

import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;

/**
 * Methods to change item object approval
 *
 * @author matt rajkowski
 * @created 8/6/18 9:05 AM
 */
public class ApproveItemCommand {

  public static void approveItem(Item item, User user) {
    if (user.hasRole("admin") || user.hasRole("data-manager")) {
      ItemRepository.approve(item, user);
    }
  }

  public static void removeItemApproval(Item item, User user) {
    if (user.hasRole("admin") || user.hasRole("data-manager")) {
      ItemRepository.removeItemApproval(item, user);
    }
  }

}
