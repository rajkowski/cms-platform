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

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ResolveHeaderLinkCommand {

  private ResolveHeaderLinkCommand() {
  }

  public static String bindParameter(String destination, String parameter, String value) {
    URI uri = URI.create(resolveLink(destination, ""));
    List<String> components = new ArrayList<>();
    if (uri.getRawQuery() != null) {
      for (String component : uri.getRawQuery().split("&")) {
        String key = component.split("=", 2)[0];
        if (!URLDecoder.decode(key, StandardCharsets.UTF_8).equals(parameter)) {
          components.add(component);
        }
      }
    }
    components.add(URLEncoder.encode(parameter, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
    String path = uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
    String fragment = uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment();
    String origin = uri.isAbsolute() ? uri.getScheme() + "://" + uri.getRawAuthority() : "";
    return URI.create(origin + path + "?" + String.join("&", components) + fragment).toString();
  }

  public static String resolveLink(String destination, String contextPath) {
    if (destination == null || destination.isBlank() || destination.startsWith("//") || destination.indexOf('\\') >= 0
        || destination.contains("${")) {
      throw new IllegalArgumentException("Header links require an internal path or an HTTP/HTTPS destination");
    }
    URI uri = URI.create(destination);
    if (uri.isAbsolute()) {
      if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException("Header links require an HTTP/HTTPS destination without embedded credentials");
      }
      return destination;
    }
    if (!destination.startsWith("/") || uri.getRawAuthority() != null) {
      throw new IllegalArgumentException("Internal header links must start with / and cannot specify another host");
    }
    for (String segment : uri.getPath().split("/")) {
      if ("..".equals(segment) || ".".equals(segment)) {
        throw new IllegalArgumentException("Internal header links cannot contain relative path segments");
      }
    }
    String prefix = contextPath == null || "/".equals(contextPath) ? "" : contextPath;
    if (prefix.isEmpty() || uri.getPath().equals(prefix) || uri.getPath().startsWith(prefix + "/")) {
      return destination;
    }
    return prefix + destination;
  }
}