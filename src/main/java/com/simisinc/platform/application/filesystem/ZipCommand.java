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

package com.simisinc.platform.application.filesystem;

import java.io.File;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Commands for working with zip files
 *
 * @author matt rajkowski
 * @created 1/19/26 10:33 AM
 */
public class ZipCommand {

  private static Log LOG = LogFactory.getLog(ZipCommand.class);

  public static void zipDirectory(File sitePath, File zipFile) {
    try {

      java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile));
      try {
        zipDirectoryRecursive(sitePath, sitePath.getName(), zos);
      } finally {
        zos.close();
      }

    } catch (Exception e) {
      LOG.error("An error occurred while zipping the directory: " + e.getMessage());
    }
  }

  private static void zipDirectoryRecursive(File sitePath, String name, ZipOutputStream zos) {
    try {
      if (sitePath.isHidden()) {
        return;
      }
      if (sitePath.isDirectory()) {
        if (name.endsWith("/")) {
          zos.putNextEntry(new java.util.zip.ZipEntry(name));
          zos.closeEntry();
        } else {
          zos.putNextEntry(new java.util.zip.ZipEntry(name + "/"));
          zos.closeEntry();
        }
        File[] children = sitePath.listFiles();
        for (File childFile : children) {
          zipDirectoryRecursive(childFile, name + "/" + childFile.getName(), zos);
        }
        return;
      }
      // It's a file
      java.io.FileInputStream fis = new java.io.FileInputStream(sitePath);
      try {
        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(name);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
          zos.write(bytes, 0, length);
        }
      } finally {
        fis.close();
      }
    } catch (Exception e) {
      LOG.error("An error occurred while zipping the directory: " + e.getMessage());
    }
  }
}
