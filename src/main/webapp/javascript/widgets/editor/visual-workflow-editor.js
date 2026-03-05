/**
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

class VisualWorkflowEditor {
  constructor(options) {
    this.token = options.token || '';
    this.viewMode = options.viewMode || 'tasks';
    this.selectedId = options.selectedId || null;
    this.tasks = [];
    this.events = [];
    this.selectedTask = null;
    this.selectedEvent = null;
    this.tasksRefreshInterval = null;
  }

  init() {
    this.setupDarkMode();
    this.setupPropertiesPanelResize();
    this.setupTabs();
    this.setupRightPanelTabs();
    this.setupEventListeners();
    this.loadInitialData();
  }

  setupDarkMode() {
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
    if (!darkModeToggle) return;

    const isDarkMode = localStorage.getItem('editor-theme') === 'dark';
    if (isDarkMode) {
      document.body.dataset.theme = 'dark';
      const icon = darkModeToggle.querySelector('i');
      if (icon) {
        icon.classList.remove('fa-moon');
        icon.classList.add('fa-sun');
      }
    }

    darkModeToggle.addEventListener('click', () => {
      const isDark = document.body.dataset.theme === 'dark';
      if (isDark) {
        delete document.body.dataset.theme;
        localStorage.setItem('editor-theme', 'light');
      } else {
        document.body.dataset.theme = 'dark';
        localStorage.setItem('editor-theme', 'dark');
      }
      const icon = darkModeToggle.querySelector('i');
      if (icon) {
        icon.classList.toggle('fa-moon', isDark);
        icon.classList.toggle('fa-sun', !isDark);
      }
    });
  }

  setupPropertiesPanelResize() {
    const handle = document.getElementById('properties-panel-resize-handle');
    const panel = document.getElementById('workflow-properties-panel');
    if (!handle || !panel) return;

    let startX, startWidth;
    handle.addEventListener('mousedown', (e) => {
      startX = e.clientX;
      startWidth = panel.offsetWidth;
      document.addEventListener('mousemove', onMouseMove);
      document.addEventListener('mouseup', onMouseUp);
      e.preventDefault();
    });

    const onMouseMove = (e) => {
      const diff = startX - e.clientX;
      const newWidth = Math.max(240, Math.min(480, startWidth + diff));
      panel.style.width = newWidth + 'px';
    };

    const onMouseUp = () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
  }

  setupTabs() {
    const tabLinks = document.querySelectorAll('#workflow-library-panel .tabs-nav a');
    tabLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const targetId = link.getAttribute('href').replace('#', '');

        // Update active tab link
        tabLinks.forEach(l => l.classList.remove('active'));
        link.classList.add('active');

        // Update active tab content
        document.querySelectorAll('#workflow-library-panel .tab-content').forEach(tab => tab.classList.remove('active'));
        const targetTab = document.getElementById(targetId);
        if (targetTab) {
          targetTab.classList.add('active');
        }

        // Switch view mode
        if (targetId === 'tasks-tab') {
          this.viewMode = 'tasks';
          this.selectedTask = null;
          this.selectedEvent = null;
          this.showTasksOverview();
        } else if (targetId === 'events-tab') {
          this.viewMode = 'events';
          this.selectedTask = null;
          this.selectedEvent = null;
          this.stopTasksAutoRefresh();
          this.showEventsOverview();
        }
      });
    });
  }

  setupRightPanelTabs() {
    const tabLinks = document.querySelectorAll('.right-panel-tabs-nav a');
    tabLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const tabId = link.getAttribute('href').replace('#', '');

        tabLinks.forEach(l => l.classList.remove('active'));
        link.classList.add('active');

        document.querySelectorAll('.right-panel-tab-content').forEach(t => t.classList.remove('active'));
        const targetTab = document.getElementById(tabId);
        if (targetTab) {
          targetTab.classList.add('active');
        }

        // If history tab selected and task active, load history
        if (tabId === 'wf-history-tab' && this.selectedTask) {
          this.loadTaskHistory(this.selectedTask.id);
        }
      });
    });
  }

  setupEventListeners() {
    const reloadBtn = document.getElementById('reload-btn');
    if (reloadBtn) {
      reloadBtn.addEventListener('click', () => this.loadInitialData());
    }
  }

  loadInitialData() {
    this.showLoading(true);
    if (this.viewMode === 'tasks') {
      this.loadTasks();
    } else {
      this.loadEvents();
    }
  }

  showLoading(show) {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) {
      indicator.style.display = show ? 'block' : 'none';
    }
  }

  loadTasks() {
    fetch('/json/workflowTasks', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
      this.showLoading(false);
      if (data.error) {
        this.showError('tasks', data.error);
        return;
      }
      this.tasks = data.jobs || [];
      this.renderTasksList();
      this.renderStatCards(data.stats || {});
      this.renderJobsTable(this.tasks);
      this.showTasksOverview();

      // Update count badge
      const countEl = document.getElementById('tasks-count');
      if (countEl) {
        countEl.textContent = this.tasks.length;
      }
    })
    .catch(err => {
      this.showLoading(false);
      console.error('Error loading tasks:', err);
      this.showError('tasks', 'Failed to load scheduled tasks');
    });
  }

  loadEvents() {
    fetch('/json/workflowEvents', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
      this.showLoading(false);
      if (data.error) {
        this.showError('events', data.error);
        return;
      }
      this.events = data.workflows || [];
      this.renderEventsList();
      this.renderEventsTable(this.events);
      this.showEventsOverview();

      // Update stat card
      const statEl = document.getElementById('stat-workflows');
      if (statEl) {
        statEl.textContent = data.total || 0;
      }

      // Update count badge
      const countEl = document.getElementById('events-count');
      if (countEl) {
        countEl.textContent = this.events.length;
      }
    })
    .catch(err => {
      this.showLoading(false);
      console.error('Error loading events:', err);
      this.showError('events', 'Failed to load event workflows');
    });
  }

  renderTasksList() {
    const list = document.getElementById('tasks-list');
    if (!list) return;

    if (this.tasks.length === 0) {
      document.getElementById('tasks-empty').style.display = 'block';
      list.style.display = 'none';
      return;
    }

    document.getElementById('tasks-empty').style.display = 'none';
    list.style.display = 'block';
    list.innerHTML = '';

    this.tasks.forEach(job => {
      const isEnqueued = job.state && job.state.toUpperCase() === 'ENQUEUED';
      const enqueuedBadge = isEnqueued ? ' <span class="job-state-badge enqueued">Enqueued</span>' : '';
      const li = document.createElement('li');
      li.className = 'workflow-list-item';
      li.dataset.jobId = job.id;
      li.innerHTML = `
        <span class="workflow-list-item-icon"><i class="far fa-clock"></i></span>
        <span class="workflow-list-item-content">
          <span class="workflow-list-item-name">${this.escapeHtml(job.name || job.id)}${enqueuedBadge}</span>
          <span class="workflow-list-item-meta">${this.escapeHtml(job.schedule || '')}</span>
        </span>
      `;
      li.addEventListener('click', () => this.selectTask(job));
      list.appendChild(li);
    });
  }

  renderEventsList() {
    const list = document.getElementById('events-list');
    if (!list) return;

    if (this.events.length === 0) {
      document.getElementById('events-empty').style.display = 'block';
      list.style.display = 'none';
      return;
    }

    document.getElementById('events-empty').style.display = 'none';
    list.style.display = 'block';
    list.innerHTML = '';

    this.events.forEach(wf => {
      const li = document.createElement('li');
      li.className = 'workflow-list-item';
      li.dataset.wfId = wf.id;
      li.innerHTML = `
        <span class="workflow-list-item-icon"><i class="far fa-bolt"></i></span>
        <span class="workflow-list-item-content">
          <span class="workflow-list-item-name">${this.escapeHtml(wf.name || wf.id)}</span>
          <span class="workflow-list-item-meta">${this.escapeHtml(wf.file || '')}</span>
        </span>
      `;
      li.addEventListener('click', () => this.selectEvent(wf));
      list.appendChild(li);
    });
  }

  renderStatCards(stats) {
    const total = document.getElementById('stat-total');
    const enqueued = document.getElementById('stat-enqueued');
    const succeeded = document.getElementById('stat-succeeded');
    const failed = document.getElementById('stat-failed');
    if (total) total.textContent = stats.total != null ? stats.total : '-';
    if (enqueued) enqueued.textContent = stats.enqueued != null ? stats.enqueued : '-';
    if (succeeded) succeeded.textContent = stats.succeeded != null ? stats.succeeded : '-';
    if (failed) failed.textContent = stats.failed != null ? stats.failed : '-';
  }

  renderJobsTable(jobs) {
    const tbody = document.getElementById('jobs-table-body');
    if (!tbody) return;
    tbody.innerHTML = '';
    jobs.forEach(job => {
      const isEnqueued = job.state && job.state.toUpperCase() === 'ENQUEUED';
      const enqueuedBadge = isEnqueued ? ' <span class="job-state-badge enqueued">Enqueued</span>' : '';
      const tr = document.createElement('tr');
      tr.dataset.jobId = job.id;
      const nextRunRelative = this.formatRelativeTime(job.nextRun);
      const nextRunFull = this.formatDateTime(job.nextRun);
      const nextRunDisplay = nextRunRelative
        ? `<span title="${this.escapeHtml(nextRunFull)}">${this.escapeHtml(nextRunRelative)}</span>`
        : '—';
      tr.innerHTML = `
        <td><code>${this.escapeHtml(job.id)}</code>${enqueuedBadge}</td>
        <td>${this.escapeHtml(job.name || job.id)}</td>
        <td><code>${this.escapeHtml(job.schedule || '')}</code></td>
        <td>${nextRunDisplay}</td>
      `;
      tr.addEventListener('click', () => this.selectTask(job));
      tbody.appendChild(tr);
    });
  }

  renderEventsTable(events) {
    const tbody = document.getElementById('events-table-body');
    if (!tbody) return;
    tbody.innerHTML = '';
    events.forEach(wf => {
      const tr = document.createElement('tr');
      tr.dataset.wfId = wf.id;
      tr.innerHTML = `
        <td><code>${this.escapeHtml(wf.id)}</code></td>
        <td>${this.escapeHtml(wf.file || '')}</td>
        <td>${wf.varCount || 0}</td>
        <td>${wf.stepCount || 0}</td>
      `;
      tr.addEventListener('click', () => this.selectEvent(wf));
      tbody.appendChild(tr);
    });
  }

  showTasksOverview() {
    document.getElementById('workflow-empty-canvas').style.display = 'none';
    document.getElementById('tasks-overview').style.display = 'block';
    document.getElementById('task-detail').style.display = 'none';
    document.getElementById('events-overview').style.display = 'none';
    document.getElementById('event-flowchart').style.display = 'none';

    const titleEl = document.getElementById('workflow-editor-title');
    if (titleEl) titleEl.textContent = 'Scheduled Tasks';

    this.startTasksAutoRefresh();
  }

  startTasksAutoRefresh() {
    this.stopTasksAutoRefresh();
    this.tasksRefreshInterval = setInterval(() => {
      if (this.viewMode === 'tasks') {
        this.loadTasks();
      } else {
        this.stopTasksAutoRefresh();
      }
    }, 30000);
  }

  stopTasksAutoRefresh() {
    if (this.tasksRefreshInterval) {
      clearInterval(this.tasksRefreshInterval);
      this.tasksRefreshInterval = null;
    }
  }

  showEventsOverview() {
    document.getElementById('workflow-empty-canvas').style.display = 'none';
    document.getElementById('tasks-overview').style.display = 'none';
    document.getElementById('task-detail').style.display = 'none';
    document.getElementById('events-overview').style.display = 'block';
    document.getElementById('event-flowchart').style.display = 'none';

    const titleEl = document.getElementById('workflow-editor-title');
    if (titleEl) titleEl.textContent = 'Event Workflows';

    // Load events if not yet loaded
    if (this.events.length === 0) {
      this.loadEvents();
    }
  }

  selectTask(job) {
    this.selectedTask = job;
    this.selectedEvent = null;

    // Highlight in list
    document.querySelectorAll('#tasks-list .workflow-list-item').forEach(li => li.classList.remove('active'));
    const activeLi = document.querySelector(`#tasks-list .workflow-list-item[data-job-id="${CSS.escape(job.id)}"]`);
    if (activeLi) activeLi.classList.add('active');

    // Highlight in table
    document.querySelectorAll('#jobs-table-body tr').forEach(tr => tr.classList.remove('active'));
    const activeTr = document.querySelector(`#jobs-table-body tr[data-job-id="${CSS.escape(job.id)}"]`);
    if (activeTr) activeTr.classList.add('active');

    // Keep the tasks overview (main panel list) visible; do NOT switch to task-detail view
    // Just populate the right-side Info / History panel

    // Update right panel Info tab
    this.updateTaskInfoPanel(job);

    // Activate the Info tab on the right panel
    const infoTabLink = document.querySelector('.right-panel-tabs-nav a[href="#wf-info-tab"]');
    if (infoTabLink) {
      document.querySelectorAll('.right-panel-tabs-nav a').forEach(l => l.classList.remove('active'));
      infoTabLink.classList.add('active');
      document.querySelectorAll('.right-panel-tab-content').forEach(t => t.classList.remove('active'));
      const infoTab = document.getElementById('wf-info-tab');
      if (infoTab) infoTab.classList.add('active');
    }

    // Load history for the History tab
    this.loadTaskHistory(job.id);
  }

  selectEvent(wf) {
    this.selectedEvent = wf;
    this.selectedTask = null;

    // Highlight in list
    document.querySelectorAll('#events-list .workflow-list-item').forEach(li => li.classList.remove('active'));
    const activeLi = document.querySelector(`#events-list .workflow-list-item[data-wf-id="${CSS.escape(wf.id)}"]`);
    if (activeLi) activeLi.classList.add('active');

    // Highlight in table
    document.querySelectorAll('#events-table-body tr').forEach(tr => tr.classList.remove('active'));
    const activeTr = document.querySelector(`#events-table-body tr[data-wf-id="${CSS.escape(wf.id)}"]`);
    if (activeTr) activeTr.classList.add('active');

    // Show flowchart in center
    document.getElementById('workflow-empty-canvas').style.display = 'none';
    document.getElementById('tasks-overview').style.display = 'none';
    document.getElementById('task-detail').style.display = 'none';
    document.getElementById('events-overview').style.display = 'none';
    document.getElementById('event-flowchart').style.display = 'block';

    const titleEl = document.getElementById('workflow-editor-title');
    if (titleEl) titleEl.textContent = wf.name || wf.id;

    document.getElementById('event-detail-name').textContent = wf.name || wf.id;
    document.getElementById('event-detail-file').textContent = wf.file || '';

    // Render flowchart
    this.renderFlowchart(wf.steps || [], wf.vars || {});

    // Update right panel
    this.updateEventInfoPanel(wf);
    this.clearHistoryPanel('Select a scheduled task to view run history');
  }

  renderFlowchart(steps, vars) {
    const container = document.getElementById('workflow-flowchart-container');
    if (!container) return;

    if (!steps || steps.length === 0) {
      container.innerHTML = '<p style="color: var(--editor-text-muted); font-size: 13px;">No steps defined</p>';
      return;
    }

    // Show workflow-level variables as a "vars" block at the top
    let topHtml = '';
    const varKeys = vars ? Object.keys(vars) : [];
    if (varKeys.length > 0) {
      topHtml += `<div class="flowchart-node">`;
      topHtml += `<div class="flowchart-node-box node-vars">`;
      topHtml += `<div class="flowchart-action-type">vars</div>`;
      varKeys.forEach(k => {
        const v = vars[k];
        topHtml += `<div class="flowchart-var-row">`;
        topHtml += `<span class="flowchart-var-key">${this.escapeHtml(k)}</span>`;
        topHtml += `<span class="flowchart-var-arrow">&#8592;</span>`;
        topHtml += `<span class="flowchart-var-val">${this.escapeHtml(String(v))}</span>`;
        topHtml += `</div>`;
      });
      topHtml += `</div>`;
      topHtml += `<div class="flowchart-connector"></div>`;
      topHtml += `<div class="flowchart-connector-arrow"></div>`;
      topHtml += `</div>`;
    }

    const html = this.buildFlowchartHtml(steps, false);
    container.innerHTML = `<div class="flowchart-nodes">${topHtml}${html}</div>`;
  }

  buildFlowchartHtml(steps, isNested) {
    let html = '';
    const indent = isNested ? 'margin-left: 30px;' : '';

    steps.forEach((step, index) => {
      const isRepeat = step.repeat && step.repeat > 0;
      const hasTasks = step.hasTasks && step.tasks && step.tasks.length > 0;
      const nodeClass = this.getFlowchartNodeClass(step, index, isNested);

      html += `<div class="flowchart-node${isRepeat ? ' node-has-loop' : ''}" style="${indent}">`;

      if (index > 0) {
        html += `<div class="flowchart-connector"></div>`;
        html += `<div class="flowchart-connector-arrow"></div>`;
      }

      html += `<div class="${nodeClass}">`;
      html += this.buildNodeBoxContent(step);
      html += `</div>`; // close node-box

      if (hasTasks) {
        html += this.buildFlowchartHtml(step.tasks, true);
      }

      if (isRepeat) {
        html += `<div class="flowchart-loop-return">`;
        html += `<div class="flowchart-loop-return-line"></div>`;
        html += `<div class="flowchart-loop-return-label">&#8617; back to start of loop</div>`;
        html += `</div>`;
      }

      html += `</div>`; // close flowchart-node
    });

    return html;
  }

  getFlowchartNodeClass(step, index, isNested) {
    const actionType = step.id || '';
    const hasWhen = step.when && step.when.trim().length > 0;
    const hasTasks = step.hasTasks && step.tasks && step.tasks.length > 0;
    let nodeClass = 'flowchart-node-box';
    if (index === 0 && !isNested) {
      nodeClass += ' node-start';
    } else if (hasTasks) {
      nodeClass += ' node-block';
    } else if (hasWhen && !actionType) {
      nodeClass += ' node-condition';
    } else if (actionType) {
      nodeClass += ' node-action';
    }
    return nodeClass;
  }

  buildNodeBoxContent(step) {
    const actionType = step.id || '';
    const stepName = step.name || step.id || 'step';
    const hasWhen = step.when && step.when.trim().length > 0;
    const hasVars = step.vars && Object.keys(step.vars).length > 0;
    const dataProps = this.parseDataString(step.data || '');
    const isRepeat = step.repeat && step.repeat > 0;
    const isParallel = step.threads && step.threads > 1;
    let html = '';

    if (actionType) {
      html += `<div class="flowchart-action-type type-${this.escapeHtml(actionType)}">${this.escapeHtml(actionType)}</div>`;
    }

    if (stepName !== actionType) {
      html += `<div class="flowchart-node-name">${this.escapeHtml(stepName)}</div>`;
    }

    if (hasWhen) {
      html += `<div class="flowchart-node-when">if: ${this.escapeHtml(step.when)}</div>`;
    }

    if (hasVars) {
      html += `<div class="flowchart-node-vars">`;
      Object.entries(step.vars).forEach(([k, v]) => {
        html += `<div class="flowchart-var-row"><span class="flowchart-var-key">${this.escapeHtml(k)}</span><span class="flowchart-var-arrow">&#8592;</span><span class="flowchart-var-val">${this.escapeHtml(String(v))}</span></div>`;
      });
      html += `</div>`;
    }

    if (dataProps.length > 0) {
      html += `<div class="flowchart-node-data">`;
      dataProps.forEach(([k, v]) => {
        html += `<div class="flowchart-data-row"><span class="flowchart-data-key">${this.escapeHtml(k)}:</span> <span class="flowchart-data-val">${this.escapeHtml(v)}</span></div>`;
      });
      html += `</div>`;
    }

    if (isRepeat) {
      html += `<div class="flowchart-loop-badge">&#8635; repeat &times; ${step.repeat}</div>`;
    }

    if (isParallel) {
      html += `<div class="flowchart-threads-badge">&#9776; ${step.threads} parallel threads</div>`;
    }

    return html;
  }

  parseDataString(data) {
    if (!data || !data.trim()) return [];
    const result = [];
    const lines = data.split('\n');
    for (const line of lines) {
      const match = line.match(/^\s*([^:]+?)\s*:\s*(.+)$/);
      if (match) {
        const key = match[1].trim();
        let val = match[2].trim();
        // Strip surrounding single or double quotes
        if ((val.startsWith("'") && val.endsWith("'")) ||
            (val.startsWith('"') && val.endsWith('"'))) {
          val = val.slice(1, -1);
        }
        if (key) {
          result.push([key, val]);
        }
      }
    }
    return result;
  }

  updateTaskInfoPanel(job) {
    const infoContent = document.getElementById('wf-info-content');
    if (!infoContent) return;
    const isEnqueued = job.state && job.state.toUpperCase() === 'ENQUEUED';
    const stateBadge = isEnqueued ? ' <span class="job-state-badge enqueued">Enqueued</span>' : '';
    const nextRunRelative = this.formatRelativeTime(job.nextRun);
    const nextRunFull = this.formatDateTime(job.nextRun);
    const nextRunDisplay = nextRunRelative
      ? `${this.escapeHtml(nextRunRelative)} <span style="color:var(--editor-text-muted); font-size:11px;">(${this.escapeHtml(nextRunFull)})</span>`
      : '—';
    infoContent.innerHTML = `
      <div class="info-item">
        <div class="info-item-label">Job ID</div>
        <div class="info-item-value"><code>${this.escapeHtml(job.id)}</code>${stateBadge}</div>
      </div>
      <div class="info-item">
        <div class="info-item-label">Job Name</div>
        <div class="info-item-value">${this.escapeHtml(job.name || job.id)}</div>
      </div>
      <div class="info-item">
        <div class="info-item-label">Schedule (Cron)</div>
        <div class="info-item-value"><code>${this.escapeHtml(job.schedule || '—')}</code></div>
      </div>
      <div class="info-item">
        <div class="info-item-label">Next Run</div>
        <div class="info-item-value">${nextRunDisplay}</div>
      </div>
    `;
  }

  updateEventInfoPanel(wf) {
    const infoContent = document.getElementById('wf-info-content');
    if (!infoContent) return;
    infoContent.innerHTML = `
      <div class="info-item">
        <div class="info-item-label">Event ID</div>
        <div class="info-item-value"><code>${this.escapeHtml(wf.id)}</code></div>
      </div>
      <div class="info-item">
        <div class="info-item-label">Source File</div>
        <div class="info-item-value">${this.escapeHtml(wf.file || '—')}</div>
      </div>
      <div class="info-item">
        <div class="info-item-label">Variables</div>
        <div class="info-item-value">${wf.varCount || 0}</div>
      </div>
      <div class="info-item">
        <div class="info-item-label">Steps</div>
        <div class="info-item-value">${wf.stepCount || 0}</div>
      </div>
    `;
  }

  clearHistoryPanel(message) {
    const historyContent = document.getElementById('wf-history-content');
    if (historyContent) {
      historyContent.innerHTML = `<p style="color: var(--editor-text-muted); font-size: 14px; padding: 12px;">${message}</p>`;
    }
  }

  loadTaskHistory(jobId) {
    const historyContent = document.getElementById('wf-history-content');
    if (!historyContent) return;

    historyContent.innerHTML = '<p style="color: var(--editor-text-muted); font-size: 13px; padding: 12px;"><i class="far fa-spinner fa-spin"></i> Loading history...</p>';

    fetch(`/json/workflowTaskHistory?jobId=${encodeURIComponent(jobId)}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
      if (data.error) {
        historyContent.innerHTML = `<p style="color: var(--editor-text-muted); font-size: 13px; padding: 12px;">${this.escapeHtml(data.error)}</p>`;
        return;
      }
      const history = data.history || [];
      if (history.length === 0) {
        historyContent.innerHTML = '<p style="color: var(--editor-text-muted); font-size: 13px; padding: 12px;">No recent history found</p>';
        return;
      }

      // Sort by updatedAt descending
      history.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));

      let html = '';
      history.forEach(item => {
        const stateClass = item.state === 'SUCCEEDED' ? 'succeeded' : 'failed';
        const timeFormatted = this.formatDateTime(item.updatedAt);
        html += `
          <div class="history-item">
            <span class="history-badge ${stateClass}">${item.state}</span>
            <span class="history-time">${this.escapeHtml(timeFormatted)}</span>
          </div>
        `;
      });
      historyContent.innerHTML = html;
    })
    .catch(err => {
      console.error('Error loading task history:', err);
      historyContent.innerHTML = '<p style="color: var(--editor-text-muted); font-size: 13px; padding: 12px;">Error loading history</p>';
    });
  }

  showError(type, message) {
    const errorEl = document.getElementById(`${type}-error`);
    if (errorEl) {
      errorEl.style.display = 'block';
      errorEl.textContent = message;
    }
  }

  formatRelativeTime(isoString) {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      if (isNaN(date.getTime())) return '';
      const diffMs = date - Date.now();
      if (diffMs <= 0) return 'now';
      const diffSec = Math.floor(diffMs / 1000);
      if (diffSec < 60) return `in ${diffSec} second${diffSec !== 1 ? 's' : ''}`;
      const diffMin = Math.floor(diffSec / 60);
      if (diffMin < 60) return `in ${diffMin} minute${diffMin !== 1 ? 's' : ''}`;
      const diffHour = Math.floor(diffMin / 60);
      if (diffHour < 24) return `in ${diffHour} hour${diffHour !== 1 ? 's' : ''}`;
      const diffDay = Math.floor(diffHour / 24);
      return `in ${diffDay} day${diffDay !== 1 ? 's' : ''}`;
    } catch (e) {
      return '';
    }
  }

  formatDateTime(isoString) {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      if (isNaN(date.getTime())) return isoString;
      return date.toLocaleString();
    } catch (e) {
      return isoString;
    }
  }

  escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(String(text)));
    return div.innerHTML;
  }
}
