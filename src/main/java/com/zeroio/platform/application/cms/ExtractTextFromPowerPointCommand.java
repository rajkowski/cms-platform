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

package com.zeroio.platform.application.cms;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xslf.extractor.XSLFExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/**
 * Command for extracting text from PowerPoint files
 *
 * @author matt rajkowski
 * @created 8/6/26 5:00 PM
 */
public class ExtractTextFromPowerPointCommand {

  private static Log LOG = LogFactory.getLog(ExtractTextFromPowerPointCommand.class);

  /**
   * Extracts text from a PowerPoint file 
   *
   * @param pptFile the PowerPoint file
   * @return the extracted text, or null if unsuccessful
   */
  public static String textFromFile(File pptFile) {
    try (FileInputStream fis = new FileInputStream(pptFile);
        XMLSlideShow ppt = new XMLSlideShow(fis);
        XSLFExtractor extractor = new XSLFExtractor(ppt)) {
      String text = extractor.getText();
      LOG.debug("Extracted text from PowerPoint file: " + pptFile.getPath());
      return text;
    } catch (Exception e) {
      LOG.warn(e.getMessage());
      return null;
    }
  }
}
