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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.simisinc.platform.presentation.controller.PageRequest;
import com.simisinc.platform.presentation.controller.UserSession;
import com.simisinc.platform.presentation.controller.WidgetContext;

class WorkspaceSelectorWidgetTest {

  @Test
  void signedOutUserReceivesSelectorJspWithoutWorkspaceList() {
    WidgetContext context = mock(WidgetContext.class);
    UserSession userSession = mock(UserSession.class);
    PageRequest request = mock(PageRequest.class);
    when(context.getUserSession()).thenReturn(userSession);
    when(context.getRequest()).thenReturn(request);
    when(userSession.isLoggedIn()).thenReturn(false);

    new WorkspaceSelectorWidget().execute(context);

    verify(context).setJsp("/cms/workspace-selector.jsp");
    verify(request, org.mockito.Mockito.never()).setAttribute(org.mockito.Mockito.anyString(), org.mockito.Mockito.any());
  }
}
