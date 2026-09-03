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

import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zeroio.platform.domain.model.tenant.DomainMapping;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.persistence.tenant.DomainMappingRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

public class WorkspaceResolutionCommand {

  private static final Log LOG = LogFactory.getLog(WorkspaceResolutionCommand.class);
  private static final String TENANT_ROUTING_ENABLED = "cms.tenant-routing.enabled";

  private WorkspaceResolutionCommand() {
  }

  public static Workspace resolveWorkspace(String host) {
    String normalizedHost = normalizeHost(host);
    if (normalizedHost == null) {
      return null;
    }
    List<DomainMapping> exactMappings = DomainMappingRepository.findByPattern(normalizedHost, false);
    if (exactMappings != null && !exactMappings.isEmpty()) {
      return resolveUnique(exactMappings);
    }
    int firstDot = normalizedHost.indexOf('.');
    if (firstDot < 1 || firstDot == normalizedHost.length() - 1) {
      return null;
    }
    return resolveUnique(DomainMappingRepository.findActiveByPattern("*." + normalizedHost.substring(firstDot + 1), true));
  }

  public static boolean isTenantRoutingEnabled() {
    return Boolean.parseBoolean(System.getProperty(TENANT_ROUTING_ENABLED, "false"));
  }

  public static String normalizeHost(String host) {
    if (host == null) {
      return null;
    }
    String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
    int portIndex = normalizedHost.indexOf(':');
    if (portIndex > -1) {
      normalizedHost = normalizedHost.substring(0, portIndex);
    }
    return normalizedHost.isBlank() || normalizedHost.contains("/") ? null : normalizedHost;
  }

  private static Workspace resolveUnique(List<DomainMapping> mappings) {
    if (mappings == null || mappings.size() != 1) {
      return null;
    }
    DomainMapping mapping = mappings.get(0);
    if (!mapping.isActive()) {
      return null;
    }
    Workspace workspace = WorkspaceRepository.findById(mapping.getWorkspaceId());
    if (workspace != null && workspace.isActive()) {
      LOG.debug("Resolved workspace " + workspace.getId() + " for domain mapping");
      return workspace;
    }
    return null;
  }
}
