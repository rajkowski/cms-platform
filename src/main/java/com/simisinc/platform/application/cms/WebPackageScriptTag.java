/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

import static com.simisinc.platform.presentation.controller.RequestConstants.CONTEXT_PATH;
import static com.simisinc.platform.presentation.controller.RequestConstants.WEB_PACKAGE_LIST;

import java.io.IOException;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.presentation.controller.WebPackage;

/**
 * Widgets reference the frontend web packages required, and later the scripts are consolidated and output
 *
 * @author matt rajkowski
 * @created 11/3/24 5:15 PM
 */
public class WebPackageScriptTag extends SimpleTagSupport {

  private static Log LOG = LogFactory.getLog(WebPackageScriptTag.class);

  private String packageName = null;
  private String file = null;
  private String charset = null;

  public void setPackage(String packageName) {
    this.packageName = packageName;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  @Override
  public void doTag() throws JspException, IOException {

    // Find the package and version
    PageContext pageContext = (PageContext) getJspContext();
    Map<String, WebPackage> webPackageList = (Map) pageContext.getAttribute(WEB_PACKAGE_LIST, PageContext.REQUEST_SCOPE);
    String version = webPackageList.get(packageName).getVersion();

    // @todo Add this package's file to the web page map so that packages can be consolidated and put in the page header

    // Just output the script element using the installed version
    if (StringUtils.isNotEmpty(version)) {
      LOG.debug("WebPackage Script Requested: " + packageName + "==" + version + ": " + file);
      String ctx = (String) pageContext.getAttribute(CONTEXT_PATH, PageContext.REQUEST_SCOPE);
      JspWriter out = pageContext.getOut();
      String charsetValue = "";
      if (charset != null) {
        charsetValue = " charset=\"" + charset + "\"";
      }
      out.println(
          "<script src=\"" + ctx + "/javascript/" + packageName + "-" + version + "/" + file + "\"" + charsetValue + "></script>");
    } else {
      LOG.error("WebPackage Resource NOT FOUND: " + packageName + ": " + file);
    }
  }
}
