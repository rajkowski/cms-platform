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

import com.simisinc.platform.infrastructure.database.DB;

/**
 * Determines user access to folders
 *
 * @author matt rajkowski
 * @created 12/14/18 1:10 PM
 */
public class CheckFolderPermissionCommand {

  public static boolean userHasAddPermission(long folderId, long userId) {
    // SELECT COUNT(*) FROM user_groups WHERE user_id = 2 AND group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = 9 AND add_permission = true);
    long count = DB.selectCountFrom("user_groups",
        DB.WHERE("user_id = ?", userId)
            .AND("group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = ? AND add_permission = true)", folderId));
    return (count > 0);
  }

  public static boolean userHasViewPermission(long folderId, long userId) {
    // SELECT COUNT(*) FROM user_groups WHERE user_id = 2 AND group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = 9 AND view_permission = true);
    long count = DB.selectCountFrom("user_groups",
        DB.WHERE("user_id = ?", userId)
            .AND("group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = ? AND view_permission = true)", folderId));
    return (count > 0);
  }

  public static boolean userHasEditPermission(long folderId, long userId) {
    // SELECT COUNT(*) FROM user_groups WHERE user_id = 2 AND group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = 9 AND edit_permission = true);
    long count = DB.selectCountFrom("user_groups",
        DB.WHERE("user_id = ?", userId)
            .AND("group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = ? AND edit_permission = true)", folderId));
    return (count > 0);
  }

  public static boolean userHasDeletePermission(long folderId, long userId) {
    // SELECT COUNT(*) FROM user_groups WHERE user_id = 2 AND group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = 9 AND delete_permission = true);
    long count = DB.selectCountFrom("user_groups",
        DB.WHERE("user_id = ?", userId)
            .AND("group_id IN (SELECT group_id FROM folder_groups WHERE folder_id = ? AND delete_permission = true)", folderId));
    return (count > 0);
  }
}
