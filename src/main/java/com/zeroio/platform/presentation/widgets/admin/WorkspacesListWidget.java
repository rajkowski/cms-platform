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
package com.zeroio.platform.presentation.widgets.admin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.infrastructure.persistence.UserSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.tenant.ActivateWorkspaceCommand;
import com.zeroio.platform.domain.model.tenant.TenantDataSourceDatabase;
import com.zeroio.platform.domain.model.tenant.TenantDataSourcePool;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.persistence.tenant.TenantDataSourceDatabaseRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.TenantDataSourcePoolRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

/**
 * Admin list of tenant workspaces.
 */
public class WorkspacesListWidget extends GenericWidget {

  static final long serialVersionUID = 1815704666384975956L;

  static String JSP = "/admin/workspaces-list.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to: " + WorkspacesListWidget.class.getSimpleName());
      return context;
    }

    List<Workspace> workspaceList = WorkspaceRepository.findAllActive();
    UserSpecification ownerSpecification = new UserSpecification();
    ownerSpecification.setIsEnabled(true);
    List<User> ownerList = UserRepository.findAll(ownerSpecification, new DataConstraints(1, 1000));

    context.getRequest().setAttribute("workspaceList", workspaceList);
    context.getRequest().setAttribute("ownerList", ownerList);
    context.getRequest().setAttribute("databaseList", loadAvailableDatabases());
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    context.setJsp(JSP);
    return context;
  }

  @Override
  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {
    if (!context.hasRole("admin")) {
      return context;
    }
    context.getUserSession().renewFormToken();

    String command = context.getParameter("command");
    if ("saveWorkspace".equals(command)) {
      return saveWorkspaceAction(context);
    }
    return context;
  }

  private WidgetContext saveWorkspaceAction(WidgetContext context) {

    // Retrieve and sanitize input parameters from the request
    String name = StringUtils.trimToNull(context.getParameter("name"));
    String uniqueId = StringUtils.lowerCase(StringUtils.trimToNull(context.getParameter("uniqueId")));
    String department = StringUtils.trimToNull(context.getParameter("department"));
    String description = StringUtils.trimToNull(context.getParameter("description"));
    long ownerId = context.getParameterAsLong("ownerId");
    long poolDatabaseId = context.getParameterAsLong("poolDatabaseId");
    context.setRedirect("/admin/workspaces");

    // Prepare the workspace bean with the retrieved parameters
    Workspace workspaceBean = new Workspace();
    workspaceBean.setUniqueId(uniqueId);
    workspaceBean.setName(name);
    workspaceBean.setDepartment(department);
    workspaceBean.setDescription(description);
    workspaceBean.setActive(true);
    if (ownerId > 0) {
      workspaceBean.setOwnerId(ownerId);
    }
    workspaceBean.setCreatedBy(context.getUserId());

    // Activate the workspace in a transaction
    String error = ActivateWorkspaceCommand.activate(workspaceBean, poolDatabaseId);
    if (error != null) {
      context.setErrorMessage(error);
      return context;
    }

    context.setSuccessMessage("Workspace was saved");
    return context;
  }

  private static List<Map<String, Object>> loadAvailableDatabases() {
    Map<Long, String> poolNames = new LinkedHashMap<>();
    for (TenantDataSourcePool pool : TenantDataSourcePoolRepository.findAll()) {
      poolNames.put(pool.getId(), pool.getUniqueId());
    }
    List<Map<String, Object>> options = new ArrayList<>();
    for (TenantDataSourceDatabase database : TenantDataSourceDatabaseRepository.findAll()) {
      if (database.isActive() || !database.isAvailable()) {
        continue;
      }
      Map<String, Object> option = new LinkedHashMap<>();
      option.put("id", database.getId());
      option.put("label",
          StringUtils.defaultString(poolNames.get(database.getPoolId())) + " / " + database.getDatabaseName());
      options.add(option);
    }
    return options;
  }
}
