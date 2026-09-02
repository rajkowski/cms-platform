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
package com.simisinc.platform.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.simisinc.platform.application.cms.BlockedIPListCommand;
import com.simisinc.platform.application.cms.HostnameCommand;
import com.simisinc.platform.application.cms.LoadBlockedIPListCommand;
import com.zaxxer.hikari.HikariDataSource;

class WebRequestFilterTest {

  @Test
  void requestUsesApplicationDataSourceDuringFilter() throws Exception {
    HikariDataSource appDataSource = mock(HikariDataSource.class);
    setApplicationDataSource(appDataSource);

    javax.sql.DataSource previousDefault = DB.getDataSource();
    javax.sql.DataSource defaultDataSource = mock(javax.sql.DataSource.class);
    DB.setDataSource(defaultDataSource);
    DB.clearTenantDataSource();

    HostnameCommand.setList("hostname-allow-list.csv", Collections.singletonList("localhost"));
    BlockedIPListCommand.setList("ip-allow-list.csv", Collections.emptyList());
    BlockedIPListCommand.setList("ip-deny-list.csv", Collections.emptyList());
    BlockedIPListCommand.setList("url-block-list.csv", Collections.emptyList());

    try (var loadBlockedIPListCommand = mockStatic(LoadBlockedIPListCommand.class)) {
      loadBlockedIPListCommand.when(LoadBlockedIPListCommand::retrieveCachedIpAddressList).thenReturn(Collections.emptyList());

      HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    ServletContext servletContext = mock(ServletContext.class);
    when(request.getServletContext()).thenReturn(servletContext);
    when(servletContext.getContextPath()).thenReturn("");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getRequestURI()).thenReturn("/favicon.ico");
    when(request.getHeader("Referer")).thenReturn(null);
    when(request.getHeader("USER-AGENT")).thenReturn("test-agent");
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getCookies()).thenReturn(new Cookie[0]);
    when(request.getSession()).thenReturn(session);
    when(request.getMethod()).thenReturn("GET");
    when(request.getHeader(SessionConstants.X_VIEW_MODE)).thenReturn(null);
    when(session.getAttribute(SessionConstants.CONTROLLER)).thenReturn(null);
    when(session.getAttribute(SessionConstants.USER)).thenReturn(null);
    when(session.getId()).thenReturn("session-1");

      FilterChain chain = (servletRequest, servletResponse) -> {
        assertSame(appDataSource, DB.getDataSource());
      };

      try {
        DB.clearTenantDataSource();
        new WebRequestFilter().doFilter(request, response, chain);
        assertNull(DB.getTenantDataSource());
      } finally {
        if (previousDefault == null) {
          DB.clearTenantDataSource();
        } else {
          DB.setDataSource(previousDefault);
        }
        DB.clearTenantDataSource();
      }
    }
  }

  private static void setApplicationDataSource(HikariDataSource dataSource) throws Exception {
    Field field = com.simisinc.platform.infrastructure.database.ConnectionPool.class.getDeclaredField("applicationDS");
    field.setAccessible(true);
    field.set(null, dataSource);
  }
}
