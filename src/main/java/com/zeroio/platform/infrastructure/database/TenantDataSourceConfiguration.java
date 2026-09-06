/*
 * Copyright 2026 Matt Rajkowski
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
package com.zeroio.platform.infrastructure.database;

/**
 * Connection properties persisted for one workspace datasource.
 */
public class TenantDataSourceConfiguration {

  private long workspaceId = -1;
  private String jdbcUrl;
  private String username;
  private String password;
  private String driverClassName;
  private String poolGroup;
  private String authMethod;
  private String azureTenantId;
  private String azureClientId;
  private String azureClientSecret;

  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getPoolGroup() {
    return poolGroup;
  }

  public void setPoolGroup(String poolGroup) {
    this.poolGroup = poolGroup;
  }

  public String getAuthMethod() {
    return authMethod;
  }

  public void setAuthMethod(String authMethod) {
    this.authMethod = authMethod;
  }

  public String getAzureTenantId() {
    return azureTenantId;
  }

  public void setAzureTenantId(String azureTenantId) {
    this.azureTenantId = azureTenantId;
  }

  public String getAzureClientId() {
    return azureClientId;
  }

  public void setAzureClientId(String azureClientId) {
    this.azureClientId = azureClientId;
  }

  public String getAzureClientSecret() {
    return azureClientSecret;
  }

  public void setAzureClientSecret(String azureClientSecret) {
    this.azureClientSecret = azureClientSecret;
  }
}