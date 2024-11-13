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
package com.simisinc.platform.presentation.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

/**
 * Resolves Thymeleaf scripts and stylesheets
 *
 * @author matt rajkowski
 * @created 11/3/24 5:47 PM
 */
public class WebPackageResolver implements IMessageResolver {

  public static final Logger LOG = LoggerFactory.getLogger(WebPackageResolver.class);

  private Map<String, WebPackage> webPackageList = null;

  public WebPackageResolver() {
    super();
  }

  /** {@inheritDoc} */
  @Override
  public String resolveMessage(ITemplateContext iTemplateContext, Class<?> aClass, String message, Object[] objects) {
    // <link rel="stylesheet" type="text/css" th:href="#{web.package('swiper', '/swiper-bundle.min.css')}" />
    // <script th:src="#{web.package('swiper', '/swiper-bundle.min.js')}"></script>
    if ("web.package".equals(message)) {
      if (objects == null || objects.length == 0) {
        return "";
      }
      String packageName = null;
      String path = null;
      if (objects.length == 1) {
        // Method 1: <link rel="stylesheet" type="text/css" th:href="#{web.package('swiper/swiper-bundle.min.css')}" />
        String value = objects[0].toString();
        if (!value.contains("/")) {
          LOG.error("Package string missing / and path");
          return "";
        }
        packageName = value.substring(0, value.indexOf("/"));
        path = value.substring(value.indexOf("/"));
      } else if (objects.length == 2) {
        // Method 2: <link rel="stylesheet" type="text/css" th:href="#{web.package('swiper', 'swiper-bundle.min.css')}" />
        packageName = objects[0].toString();
        path = objects[1].toString();
      }
      if (packageName == null || path == null) {
        LOG.error("Package or Path not set");
        return "";
      }
      WebPackage webPackage = (webPackageList.get(packageName));
      if (webPackage != null) {
        return iTemplateContext.getVariable("ctx") + "/javascript/" + webPackage.getName() + "-" + webPackage.getVersion() + "/" + path;
      }
      LOG.error("Package not found: " + packageName);
    }
    return "";
  }

  /** {@inheritDoc} */
  @Override
  public String createAbsentMessageRepresentation(ITemplateContext iTemplateContext, Class<?> aClass, String s,
      Object[] objects) {
    return String.format("key:{}", s);
  }

  /** {@inheritDoc} */
  @Override
  public Integer getOrder() {
    return 0;
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "resolver";
  }

  public Map<String, WebPackage> getWebPackageList() {
    return webPackageList;
  }

  public void setWebPackageList(Map<String, WebPackage> webPackageList) {
    this.webPackageList = webPackageList;
  }

}