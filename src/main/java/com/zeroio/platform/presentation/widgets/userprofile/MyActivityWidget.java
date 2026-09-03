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
package com.zeroio.platform.presentation.widgets.userprofile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.xapi.XapiStatement;
import com.simisinc.platform.infrastructure.persistence.xapi.XapiStatementRepository;
import com.simisinc.platform.infrastructure.persistence.xapi.XapiStatementSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Shows account activity history for the logged-in user.
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class MyActivityWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/userProfile/my-activity.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));

    // This widget is for a logged in user
    if (!context.getUserSession().isLoggedIn()) {
      return null;
    }

    User user = context.getUserSession().getUser();
    context.getRequest().setAttribute("user", user);

    // Load account activity history related to this user.
    DataConstraints constraints = new DataConstraints(1, 50);
    constraints.setColumnToSortBy("occurred_at", "desc");

    List<XapiStatement> statementList = new ArrayList<>();
    Map<Long, XapiStatement> mergedStatements = new LinkedHashMap<>();

    XapiStatementSpecification actorSpecification = new XapiStatementSpecification();
    actorSpecification.setActorId(user.getId());
    List<XapiStatement> actorStatements = XapiStatementRepository.findAll(actorSpecification, constraints);
    if (actorStatements != null) {
      for (XapiStatement statement : actorStatements) {
        mergedStatements.put(statement.getId(), statement);
      }
    }

    XapiStatementSpecification userObjectSpecification = new XapiStatementSpecification();
    userObjectSpecification.setObject("user");
    userObjectSpecification.setObjectId(user.getId());
    List<XapiStatement> userObjectStatements = XapiStatementRepository.findAll(userObjectSpecification, constraints);
    if (userObjectStatements != null) {
      for (XapiStatement statement : userObjectStatements) {
        mergedStatements.put(statement.getId(), statement);
      }
    }

    statementList.addAll(mergedStatements.values());
    statementList.sort(Comparator.comparing(XapiStatement::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    if (statementList.size() > 50) {
      statementList = statementList.subList(0, 50);
    }

    context.getRequest().setAttribute("xapiStatementList", statementList);

    // Determine if the widget is shown
    if (!showWhenEmpty && statementList.isEmpty()) {
      return context;
    }

    context.setJsp(JSP);
    return context;
  }
}
