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

package com.simisinc.platform.application.cms;

import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.cms.MenuItem;
import com.simisinc.platform.infrastructure.persistence.cms.MenuItemRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Loads a list of menu item objects
 *
 * @author matt rajkowski
 * @created 5/1/18 11:43 AM
 */
public class LoadMenuItemsCommand {

  private static Log LOG = LogFactory.getLog(LoadMenuItemsCommand.class);

  public static List<MenuItem> findAllActiveByMenuTab(MenuTab menuTab) {
    return MenuItemRepository.findAllActiveByMenuTab(menuTab);
  }

  public static List<MenuItem> findAllByMenuTab(MenuTab menuTab) {
    return MenuItemRepository.findAllByMenuTab(menuTab);
  }
}
