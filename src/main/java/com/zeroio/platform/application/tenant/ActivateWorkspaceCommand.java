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
package com.zeroio.platform.application.tenant;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.simisinc.platform.application.admin.DatabaseCommand;
import com.zeroio.platform.application.cms.WorkspaceResolutionCommand;
import com.zeroio.platform.domain.model.tenant.TenantDataSourceDatabase;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.persistence.tenant.TenantDataSourceDatabaseRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

public class ActivateWorkspaceCommand {

    // Used as a subdomain and a folder name
    private static final Pattern UNIQUE_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");

    public static synchronized String activate(Workspace workspaceBean, long poolDatabaseId) {

        // Validate the values
        String name = workspaceBean.getName();
        String uniqueId = workspaceBean.getUniqueId();
        String department = workspaceBean.getDepartment();
        String description = workspaceBean.getDescription();
        long ownerId = workspaceBean.getOwnerId();
        long createdBy = workspaceBean.getCreatedBy();

        if (name == null || uniqueId == null || poolDatabaseId < 1) {
            return "Name, unique id, and database are required.";
        }
        if (!UNIQUE_ID_PATTERN.matcher(uniqueId).matches()) {
            return "The unique id must be 3-63 characters of lowercase letters, numbers, and hyphens, and cannot start or end with a hyphen.";
        }
        if (WorkspaceRepository.findByUniqueId(uniqueId) != null) {
            return "A workspace with that unique id already exists.";
        }
        if (createdBy < 1) {
            return "The creator of the workspace must be specified.";
        }

        // Confirm the database is still free to allocate
        TenantDataSourceDatabase database = TenantDataSourceDatabaseRepository.findById(poolDatabaseId);
        if (database == null || database.isActive() || !database.isAvailable()) {
            return "The selected database is not available.";
        }

        // Generate the host values from the unique id and the default workspace url
        String defaultUrl = StringUtils.trimToNull(WorkspaceResolutionCommand.getDefaultWorkspaceUrl());
        String defaultHost = WorkspaceResolutionCommand.normalizeHost(defaultUrl);
        if (defaultUrl == null || defaultHost == null) {
            return "The default workspace url is not configured, so the workspace url cannot be generated.";
        }
        String canonicalDomain = uniqueId + "." + defaultHost;
        if (WorkspaceRepository.findByCanonicalDomain(canonicalDomain) != null) {
            return "A workspace with that domain already exists.";
        }

        int schemeEnd = defaultUrl.indexOf("://");
        String scheme = schemeEnd > -1 ? defaultUrl.substring(0, schemeEnd + 3) : "https://";
        String hostAndPort = schemeEnd > -1 ? defaultUrl.substring(schemeEnd + 3) : defaultUrl;
        int pathIndex = hostAndPort.indexOf('/');
        if (pathIndex > -1) {
            hostAndPort = hostAndPort.substring(0, pathIndex);
        }
        String siteUrl = scheme + uniqueId + "." + hostAndPort;

        // Determine the file root for the workspace
        String cmsPath = StringUtils.defaultIfBlank(System.getenv("CMS_PATH"), "/workspace");
        String fileRoot = Strings.CS.removeEnd(cmsPath, "/") + "/workspaces/" + uniqueId;
        // Make the directory and parents
        File fileRootDir = new File(fileRoot);
        if (!fileRootDir.exists()) {
            if (!fileRootDir.mkdirs()) {
                return "Failed to create workspace file root directory: " + fileRoot;
            }
        }

        // Have Flyway check the database schema
        if (!DatabaseCommand.checkPoolDatabase(database)) {
            return "The selected database could not be verified.";
        }

        // Set properties in the new site
        // site.name
        // site.url
        // site.description

        // system.configpath
        // system.filepath
        // system.customizations.filepath
        // system.staticsite.filepath

        // mail.from_name
        // mail.from_address

        // in the seed: site.online = true

        // Activate the workspace
        Workspace workspace = new Workspace();
        workspace.setUniqueId(uniqueId);
        workspace.setName(name);
        workspace.setSiteUrl(siteUrl);
        workspace.setCanonicalDomain(canonicalDomain);
        workspace.setFileRoot(fileRoot);
        workspace.setDepartment(department);
        workspace.setDescription(description);
        workspace.setActive(true);
        if (ownerId > 0) {
            workspace.setOwnerId(ownerId);
        }
        workspace.setCreatedBy(createdBy);

        // Saves the workspace, maps the database, and marks it active/unavailable
        // atomically
        Workspace saved = WorkspaceRepository.saveAndAllocateDatabase(workspace, poolDatabaseId);
        if (saved == null) {
            return "Workspace could not be saved. Please review the values and try again.";
        }

        return null; // Return an error message if activation fails, otherwise return null
    }
}
