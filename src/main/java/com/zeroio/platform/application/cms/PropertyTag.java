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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.HtmlCommand;

/**
 * Outputs the value of a site property as HTML
 *
 * @author matt rajkowski
 */
public class PropertyTag extends SimpleTagSupport {

  private String value = null;

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public void doTag() throws JspException, IOException {

    PageContext pageContext = (PageContext) getJspContext();
    String propertyValue = LoadSitePropertyCommand.loadByName(value);

    // Output the script tag
    JspWriter out = pageContext.getOut();
    out.println(HtmlCommand.toHtml(propertyValue));
  }
}
