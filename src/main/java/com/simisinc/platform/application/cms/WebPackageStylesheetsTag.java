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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Outputs all of the common stylesheets specified within widgets
 *
 * @author matt rajkowski
 * @created 11/3/24 5:15 PM
 */
public class WebPackageStylesheetsTag extends SimpleTagSupport {

  private static Log LOG = LogFactory.getLog(WebPackageStylesheetsTag.class);

  @Override
  public void doTag() throws JspException, IOException {

    // Find the package, version, and file
    // PageContext pageContext = (PageContext) getJspContext();

    // Iterate the Stylesheets and output them... this allows them to be in the head, and removes duplication

    // @todo if there are any to output
    // LOG.debug("WebPackage Stylesheet Requested: " + packageName + "==" + version + ": " + file);
    // String ctx = (String) pageContext.getAttribute(CONTEXT_PATH, PageContext.REQUEST_SCOPE);
    // JspWriter out = pageContext.getOut();
    // out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + ctx + "/javascript/" + packageName + "-" + version + "/" + file + "\" />");
  }
}
