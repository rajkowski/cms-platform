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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.simisinc.platform.domain.model.admin.PermissionGroup;

/**
 * Loads Cedar permission group configuration from WEB-INF/permission-groups/.
 *
 * <p>Reads {@code component-groups.xml} to map component class names to group codes,
 * then loads the corresponding {@code .cedar} policy files from the {@code policies/}
 * subdirectory. Call {@link #mergeDbOverrides} after loading to apply runtime DB
 * overrides before passing the list to {@link PermissionEngine#load}.
 *
 * @author matt rajkowski
 * @created 3/6/26 8:00 AM
 */
public class PermissionLoader {

  private static Log LOG = LogFactory.getLog(PermissionLoader.class);

  private static final String COMPONENT_GROUPS_FILE = "/WEB-INF/permission-groups/component-groups.xml";
  private static final String POLICIES_DIR = "/WEB-INF/permission-groups/policies/";

  /**
   * Loads all permission groups from the WEB-INF config files.
   *
   * @param context the servlet context used to read resources
   * @return list of PermissionGroup objects ready for {@link PermissionEngine#load}
   */
  public static List<PermissionGroup> load(ServletContext context) {
    // 1. Parse component-groups.xml → build a group-code → PermissionGroup map
    Map<String, PermissionGroup> groupByCode = parseComponentGroups(context);
    if (groupByCode.isEmpty()) {
      LOG.warn("PermissionLoader: no components found in " + COMPONENT_GROUPS_FILE);
      return new ArrayList<>();
    }

    // 2. Load .cedar policy text for each group
    loadPolicies(context, groupByCode);

    // 3. Remove groups that have no policy text (misconfiguration guard)
    List<PermissionGroup> result = new ArrayList<>();
    for (PermissionGroup group : groupByCode.values()) {
      if (StringUtils.isBlank(group.getCedarPolicyText())) {
        LOG.warn("PermissionLoader: no .cedar policy found for group '" + group.getCode() + "' — skipping");
      } else {
        result.add(group);
        LOG.info(
            "PermissionLoader: loaded group '" + group.getCode() + "' with " + group.getMemberClassNames().size() + " component(s)");
      }
    }
    return result;
  }

  /**
   * Merges database policy overrides into an existing list of permission groups.
   *
   * <ul>
   *   <li>DB rows whose {@code group_code} matches an XML-loaded group REPLACE
   *       the Cedar policy text (and member list if DB members are present).</li>
   *   <li>DB rows with codes not in the XML list ADD entirely new groups.</li>
   * </ul>
   *
   * @param xmlGroups   mutable list from {@link #load} (modified in-place)
   * @param dbPolicies  rows from {@code permission_policies} table
   * @param dbMembers   map of group_code → list of {className, memberType} pairs
   */
  public static void mergeDbOverrides(List<PermissionGroup> xmlGroups,
      List<PermissionGroup> dbPolicies, Map<String, List<String[]>> dbMembers) {
    if (dbPolicies == null || dbPolicies.isEmpty()) {
      return;
    }
    Map<String, PermissionGroup> xmlByCode = new LinkedHashMap<>();
    for (PermissionGroup group : xmlGroups) {
      xmlByCode.put(group.getCode(), group);
    }
    for (PermissionGroup dbGroup : dbPolicies) {
      if (!dbGroup.isEnabled()) {
        continue;
      }
      String code = dbGroup.getCode();
      if (xmlByCode.containsKey(code)) {
        PermissionGroup existing = xmlByCode.get(code);
        existing.setCedarPolicyText(dbGroup.getCedarPolicyText());
        LOG.info("PermissionLoader: DB override applied for group '" + code + "'");
        if (dbMembers.containsKey(code)) {
          existing.getMemberClassNames().clear();
          existing.getMemberTypes().clear();
          for (String[] member : dbMembers.get(code)) {
            existing.addMemberClassName(member[0], member[1]);
          }
        }
      } else {
        java.util.List<String[]> members = dbMembers.getOrDefault(code, java.util.Collections.emptyList());
        for (String[] member : members) {
          dbGroup.addMemberClassName(member[0], member[1]);
        }
        xmlGroups.add(dbGroup);
        LOG.info("PermissionLoader: DB-only group '" + code + "' added with " + members.size() + " component(s)");
      }
    }
  }

  private static Map<String, PermissionGroup> parseComponentGroups(ServletContext context) {
    Map<String, PermissionGroup> groupByCode = new LinkedHashMap<>();
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      DocumentBuilder builder = factory.newDocumentBuilder();

      try (InputStream is = context.getResourceAsStream(COMPONENT_GROUPS_FILE)) {
        if (is == null) {
          LOG.warn("PermissionLoader: " + COMPONENT_GROUPS_FILE + " not found");
          return groupByCode;
        }
        Document doc = builder.parse(is);
        NodeList components = doc.getElementsByTagName("component");
        for (int i = 0; i < components.getLength(); i++) {
          Element el = (Element) components.item(i);
          String className = el.getAttribute("class");
          String groupCode = el.getAttribute("group");
          String type = el.getAttribute("type");
          if (StringUtils.isBlank(className) || StringUtils.isBlank(groupCode)) {
            LOG.warn("PermissionLoader: skipping component with missing class or group attribute");
            continue;
          }
          PermissionGroup group = groupByCode.computeIfAbsent(groupCode, code -> {
            PermissionGroup g = new PermissionGroup();
            g.setCode(code);
            g.setName(code.replace('-', ' '));
            return g;
          });
          group.addMemberClassName(className, StringUtils.defaultIfBlank(type, "WIDGET"));
          LOG.debug("PermissionLoader: mapped " + className + " → " + groupCode);
        }
      }
    } catch (Exception e) {
      LOG.error("PermissionLoader: failed to parse " + COMPONENT_GROUPS_FILE, e);
    }
    return groupByCode;
  }

  private static void loadPolicies(ServletContext context, Map<String, PermissionGroup> groupByCode) {
    Set<String> policyFiles = context.getResourcePaths(POLICIES_DIR);
    if (policyFiles == null || policyFiles.isEmpty()) {
      LOG.warn("PermissionLoader: no policy files found in " + POLICIES_DIR);
      return;
    }
    for (String filePath : policyFiles) {
      if (!filePath.endsWith(".cedar")) {
        continue;
      }
      // Derive group code from filename: "visual-web-sync-editor.cedar" → "visual-web-sync-editor"
      String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
      String groupCode = fileName.substring(0, fileName.length() - ".cedar".length());
      if (!groupByCode.containsKey(groupCode)) {
        LOG.debug("PermissionLoader: policy file '" + fileName + "' has no registered components — ignoring");
        continue;
      }
      try (InputStream is = context.getResourceAsStream(filePath);
          BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        String policyText = reader.lines().collect(Collectors.joining("\n"));
        groupByCode.get(groupCode).setCedarPolicyText(policyText);
        LOG.debug("PermissionLoader: loaded policy for group '" + groupCode + "'");
      } catch (Exception e) {
        LOG.error("PermissionLoader: failed to read policy file " + filePath, e);
      }
    }
  }
}
