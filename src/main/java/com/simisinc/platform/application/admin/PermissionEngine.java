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
package com.simisinc.platform.application.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.admin.PermissionGroup;
import com.simisinc.platform.presentation.controller.UserSession;

/**
 * Central permission engine using Cedar policy files as the source of truth.
 *
 * <p>The engine performs pure-Java evaluation of Cedar {@code permit} policies.
 * The supported Cedar condition subset is:
 * <pre>
 *   when {
 *     "roleName" in principal.roles
 *     "groupId"  in principal.groups
 *   }
 * </pre>
 * Multiple conditions may be joined with {@code ||} (OR) or {@code &amp;&amp;} (AND).
 *
 * <p>If a component's class name is not registered, {@code checkAccess} returns
 * {@code false} (closed by default — the engine restricts classes).
 *
 * @author matt rajkowski
 * @created 3/6/26 8:00 AM
 */
public class PermissionEngine {

  private static Log LOG = LogFactory.getLog(PermissionEngine.class);

  // Keyed by fully-qualified class name → its governing PermissionGroup
  private static Map<String, PermissionGroup> componentMap = Collections.emptyMap();

  // Keyed by group code → PermissionGroup (for admin display)
  private static Map<String, PermissionGroup> groupMap = Collections.emptyMap();

  // Parses:  "someValue" in principal.roles   or   "someValue" in principal.groups
  private static final Pattern CONDITION_PATTERN = Pattern.compile("\"([^\"]+)\"\\s+in\\s+principal\\.(roles|groups)");

  /**
   * Loads groups into the engine. Replaces any previously loaded configuration.
   * Called once at startup (XML) and optionally again after DB merge.
   */
  public static synchronized void load(List<PermissionGroup> groups) {
    Map<String, PermissionGroup> newGroupMap = new LinkedHashMap<>();
    Map<String, PermissionGroup> newComponentMap = new LinkedHashMap<>();
    for (PermissionGroup group : groups) {
      newGroupMap.put(group.getCode(), group);
      for (String className : group.getMemberClassNames()) {
        newComponentMap.put(className, group);
      }
    }
    groupMap = Collections.unmodifiableMap(newGroupMap);
    componentMap = Collections.unmodifiableMap(newComponentMap);
    LOG.info("PermissionEngine loaded " + newGroupMap.size() + " groups covering " + newComponentMap.size() + " components");
  }

  /**
   * Checks whether the given user session is permitted to access the named component class.
   *
   * @param className   fully-qualified class name of the widget or service
   * @param userSession current user session
   * @return {@code true} if permitted (or if the class is not governed by any policy)
   */
  public static boolean checkAccess(String className, UserSession userSession) {
    PermissionGroup group = componentMap.get(className);
    if (group == null) {
      // Not registered — disallow by default
      return false;
    }
    return evaluate(group.getCedarPolicyText(), userSession);
  }

  /**
   * Returns an unmodifiable list of all loaded permission groups (for admin display).
   */
  public static List<PermissionGroup> getAllGroups() {
    return new ArrayList<>(groupMap.values());
  }

  /**
   * Returns the permission group for a given code, or {@code null} if not found.
   */
  public static PermissionGroup findGroup(String code) {
    return groupMap.get(code);
  }

  // -----------------------------------------------------------------------
  // Pure-Java Cedar evaluation
  // -----------------------------------------------------------------------

  /**
   * Evaluates a Cedar {@code permit} policy's {@code when} block against the user session.
   *
   * <p>The evaluator:
   * <ol>
   *   <li>Extracts the {@code when { ... }} block from the policy text.</li>
   *   <li>Splits on {@code ||} and {@code &amp;&amp;} to obtain individual conditions.</li>
   *   <li>For each condition of the form {@code "value" in principal.roles} or
   *       {@code "value" in principal.groups}, checks the user session.</li>
   *   <li>Combines results: {@code ||}-separated groups are OR-ed; within a group,
   *       {@code &amp;&amp;}-conditions are AND-ed.</li>
   * </ol>
   *
   * @param policyText the full Cedar policy text
   * @param userSession the user to evaluate against
   * @return {@code true} if the policy permits access
   */
  static boolean evaluate(String policyText, UserSession userSession) {
    if (StringUtils.isBlank(policyText)) {
      return false;
    }
    if (userSession == null || !userSession.isLoggedIn()) {
      return false;
    }

    String whenBlock = extractWhenBlock(policyText);
    if (whenBlock == null) {
      // No when clause — bare permit applies to everyone logged in
      return true;
    }

    // Split into OR groups first, then AND within each group
    String[] orGroups = whenBlock.split("\\|\\|");
    for (String orGroup : orGroups) {
      String[] andConditions = orGroup.split("&&");
      boolean andResult = true;
      for (String condition : andConditions) {
        if (!evaluateCondition(condition.trim(), userSession)) {
          andResult = false;
          break;
        }
      }
      if (andResult) {
        return true;
      }
    }
    return false;
  }

  private static String extractWhenBlock(String policyText) {
    int whenIndex = policyText.indexOf("when");
    if (whenIndex == -1) {
      return null;
    }
    int openBrace = policyText.indexOf('{', whenIndex);
    int closeBrace = policyText.lastIndexOf('}');
    if (openBrace == -1 || closeBrace == -1 || closeBrace <= openBrace) {
      return null;
    }
    // There may be a trailing '}' and ';' from the outer permit block — take the first close brace
    // that pairs with the when-open brace
    int depth = 0;
    for (int i = openBrace; i < policyText.length(); i++) {
      char c = policyText.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          closeBrace = i;
          break;
        }
      }
    }
    return policyText.substring(openBrace + 1, closeBrace);
  }

  private static boolean evaluateCondition(String condition, UserSession userSession) {
    if (StringUtils.isBlank(condition)) {
      return true;
    }
    Matcher m = CONDITION_PATTERN.matcher(condition);
    if (!m.find()) {
      LOG.warn("PermissionEngine: unrecognized condition clause, denying: " + condition);
      return false;
    }
    String value = m.group(1);
    String attribute = m.group(2);
    if ("roles".equals(attribute)) {
      return userSession.hasRole(value);
    } else if ("groups".equals(attribute)) {
      return userSession.hasGroup(value);
    }
    return false;
  }
}
