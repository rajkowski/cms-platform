/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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
package com.simisinc.platform.infrastructure.web;

import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * For accessing web application server resources
 */
public class WebApp {

  private static Log LOG = LogFactory.getLog(WebApp.class);

  private static ServletContext context;

  private WebApp() {

  }

  public static void init(ServletContext servletContext) {
    context = servletContext;
  }

  public static void shutdown() {
    context = null;
  }

  public static URL getResource(String path) {
    try {
      return context.getResource(path);
    } catch (Exception e) {
      return null;
    }
  }

  public static InputStream getResourceAsStream(String path) {
    try {
      return context.getResourceAsStream(path);
    } catch (Exception e) {
      return null;
    }
  }

}
