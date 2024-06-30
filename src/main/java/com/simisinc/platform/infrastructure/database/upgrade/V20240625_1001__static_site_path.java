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

package com.simisinc.platform.infrastructure.database.upgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.simisinc.platform.application.filesystem.FileSystemCommand;

/**
 * Sets the newly created static site filepath
 *
 * @author matt rajkowski
 * @created 6/25/2024 7:25 AM
 */
public class V20240625_1001__static_site_path extends BaseJavaMigration {

  private static final String CMS_PATH = "CMS_PATH";

  @Override
  public void migrate(Context context) throws Exception {

    Connection connection = context.getConnection();

    // A runtime environment variable is used in place of the database value, if not found, this database value is used
    // Determine the OS, set a default path for files
    File filesPath = FileSystemCommand.getFileServerRootPath();

    // Set the static site path
    File staticSitePath = new File(filesPath.getParentFile(), "static-site");
    PreparedStatement pst = connection.prepareStatement(
        "UPDATE site_properties SET property_value = ? WHERE property_name = ?");
    try {
      pst.setString(1, staticSitePath.getPath());
      pst.setString(2, "system.staticsite.filepath");
      pst.execute();
    } finally {
      pst.close();
    }
  }
}
