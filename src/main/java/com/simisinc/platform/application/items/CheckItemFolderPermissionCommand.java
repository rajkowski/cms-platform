/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

import com.github.rajkowski.database.DB;

/**
 * Methods to check item folder object permissions
 *
 * @author matt rajkowski
 * @created 4/19/2021 1:00 PM
 */
public class CheckItemFolderPermissionCommand {

  public static boolean userHasAddPermission(long folderId, long userId) {
    long count = DB.SELECT("COUNT(*)")
        .FROM("user_groups")
        .WHERE("user_id = ?", userId)
        .AND("group_id IN (SELECT group_id FROM item_folder_groups WHERE folder_id = ? AND add_permission = ?)", folderId, true)
        .returnCount();
    return count > 0;
  }

  public static boolean userHasViewPermission(long folderId, long userId) {
    long count = DB.SELECT("COUNT(*)")
        .FROM("user_groups")
        .WHERE("user_id = ?", userId)
        .AND("group_id IN (SELECT group_id FROM item_folder_groups WHERE folder_id = ? AND view_permission = ?)", folderId, true)
        .returnCount();
    return count > 0;
  }

  public static boolean userHasEditPermission(long folderId, long userId) {
    long count = DB.SELECT("COUNT(*)")
        .FROM("user_groups")
        .WHERE("user_id = ?", userId)
        .AND("group_id IN (SELECT group_id FROM item_folder_groups WHERE folder_id = ? AND edit_permission = ?)", folderId, true)
        .returnCount();
    return count > 0;
  }

  public static boolean userHasDeletePermission(long folderId, long userId) {
    long count = DB.SELECT("COUNT(*)")
        .FROM("user_groups")
        .WHERE("user_id = ?", userId)
        .AND("group_id IN (SELECT group_id FROM item_folder_groups WHERE folder_id = ? AND delete_permission = ?)", folderId, true)
        .returnCount();
    return count > 0;
  }
}
