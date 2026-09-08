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
package com.zeroio.platform.presentation.widgets.cms;

import java.util.List;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.login.WorkspaceAccessCommand;
import com.zeroio.platform.domain.model.tenant.Workspace;

public class WorkspaceSelectorWidget extends GenericWidget {

  private static final long serialVersionUID = 1L;
  private static final String JSP = "/cms/workspace-selector.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {
    context.setJsp(JSP);
    if (context.getUserSession() == null || !context.getUserSession().isLoggedIn()) {
      return context;
    }
    List<Workspace> workspaces = WorkspaceAccessCommand.findAuthorizedWorkspaces(context.getUserSession().getUserId());
    context.getRequest().setAttribute("workspaceList", workspaces);
    return context;
  }
}
