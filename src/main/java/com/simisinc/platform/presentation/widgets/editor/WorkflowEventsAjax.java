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

package com.simisinc.platform.presentation.widgets.editor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.Task;
import org.jeasy.flows.playbook.TaskList;
import org.jeasy.flows.reader.YamlReader;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns event workflow information for the visual workflow editor
 *
 * @author matt rajkowski
 * @created 02/27/26 9:00 AM
 */
public class WorkflowEventsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908901L;
  private static Log LOG = LogFactory.getLog(WorkflowEventsAjax.class);

  private static final String WORKFLOWS_PATH = "/WEB-INF/workflows";

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("WorkflowEventsAjax...");

    // Check permissions
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to access workflow events");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Read workflow files from the servlet context
    List<WorkflowEntry> workflows = new ArrayList<>();
    try {
      ServletContext servletContext = SchedulerManager.getServletContext();
      if (servletContext == null) {
        servletContext = context.getServletContext();
      }
      if (servletContext != null) {
        Set<String> paths = servletContext.getResourcePaths(WORKFLOWS_PATH);
        if (paths != null) {
          for (String filePath : paths) {
            if (!filePath.contains("-playbook") && !filePath.contains("-workflow")) {
              continue;
            }
            try (InputStream inputStream = servletContext.getResourceAsStream(filePath)) {
              if (inputStream == null) {
                continue;
              }
              String yaml = IOUtils.toString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
              List<Playbook> playbookList = YamlReader.readPlaybooks(yaml);
              if (playbookList != null) {
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                for (Playbook playbook : playbookList) {
                  int varCount = playbook.getVars() != null ? playbook.getVars().size() : 0;
                  int stepCount = countTasks(playbook.getTaskList());
                  String stepsJson = buildTasksJson(playbook.getTaskList());
                  String varsJson = buildVarsJson(playbook.getVars());
                  workflows.add(
                      new WorkflowEntry(playbook.getId(), playbook.getName(), fileName, varCount, stepCount, stepsJson, varsJson));
                }
              }
            } catch (Exception e) {
              LOG.warn("Could not read workflow file: " + filePath + " - " + e.getMessage());
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Could not read workflow events: " + e.getMessage());
    }

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"total\":").append(workflows.size()).append(",");
    sb.append("\"workflows\":[");

    boolean first = true;
    for (WorkflowEntry entry : workflows) {
      if (!first) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"id\":\"").append(JsonCommand.toJson(entry.id)).append("\",");
      String displayName = StringUtils.isNotBlank(entry.name) ? entry.name : entry.id;
      sb.append("\"name\":\"").append(JsonCommand.toJson(displayName)).append("\",");
      sb.append("\"file\":\"").append(JsonCommand.toJson(entry.file)).append("\",");
      sb.append("\"varCount\":").append(entry.varCount).append(",");
      sb.append("\"stepCount\":").append(entry.stepCount).append(",");
      sb.append("\"vars\":").append(entry.varsJson).append(",");
      sb.append("\"steps\":").append(entry.stepsJson);
      sb.append("}");
      first = false;
    }

    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }

  private int countTasks(TaskList taskList) {
    if (taskList == null) {
      return 0;
    }
    int count = 0;
    for (Task task : taskList) {
      count++;
      if (task.hasTasks()) {
        count += countTasks(task.getTaskList());
      }
    }
    return count;
  }

  private String buildTasksJson(TaskList taskList) {
    if (taskList == null || taskList.isEmpty()) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (Task task : taskList) {
      if (!first) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"id\":\"").append(JsonCommand.toJson(task.getId() != null ? task.getId() : "")).append("\",");
      String taskId = task.getId() != null ? task.getId() : "step";
      String taskName = task.getName() != null ? task.getName() : taskId;
      sb.append("\"name\":\"").append(JsonCommand.toJson(taskName)).append("\",");
      sb.append("\"when\":\"").append(JsonCommand.toJson(task.getWhen() != null ? task.getWhen() : "")).append("\",");
      sb.append("\"data\":\"").append(JsonCommand.toJson(task.getData() != null ? task.getData() : "")).append("\",");
      sb.append("\"vars\":").append(buildVarsJson(task.getVars())).append(",");
      sb.append("\"repeat\":").append(task.getRepeat()).append(",");
      sb.append("\"threads\":").append(task.getThreads()).append(",");
      sb.append("\"hasTasks\":").append(task.hasTasks());
      if (task.hasTasks()) {
        sb.append(",\"tasks\":").append(buildTasksJson(task.getTaskList()));
      }
      sb.append("}");
      first = false;
    }
    sb.append("]");
    return sb.toString();
  }

  private String buildVarsJson(java.util.Map<String, Object> vars) {
    if (vars == null || vars.isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (java.util.Map.Entry<String, Object> entry : vars.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      sb.append("\"").append(JsonCommand.toJson(entry.getKey())).append("\":\"")
          .append(JsonCommand.toJson(entry.getValue() != null ? String.valueOf(entry.getValue()) : "")).append("\"");
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }

  private static class WorkflowEntry {
    final String id;
    final String name;
    final String file;
    final int varCount;
    final int stepCount;
    final String stepsJson;
    final String varsJson;

    WorkflowEntry(String id, String name, String file, int varCount, int stepCount, String stepsJson, String varsJson) {
      this.id = id;
      this.name = name;
      this.file = file;
      this.varCount = varCount;
      this.stepCount = stepCount;
      this.stepsJson = stepsJson;
      this.varsJson = varsJson;
    }
  }
}
