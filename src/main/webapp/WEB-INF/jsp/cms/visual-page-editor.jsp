<%--
  ~ Copyright 2025 Matt Rajkowski
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="webPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<web:stylesheet package="spectrum" file="spectrum.css" />
<web:stylesheet package="dragula" file="dragula.min.css" />
<web:script package="spectrum" file="spectrum.js" />
<web:script package="tinymce" file="tinymce.min.js" />

<g:compress>
<style>
  /* Light and Dark Mode Variables */
  :root {
    --editor-bg: #ffffff;
    --editor-panel-bg: #f8f9fa;
    --editor-panel-input: #ffffff;
    --editor-panel-input-placeholder: #999999;
    --editor-border: #dee2e6;
    --editor-text: #212529;
    --editor-text-muted: #6c757d;
    --editor-hover-bg: #f8f9fa;
    --editor-selected-bg: #f0f7ff;
    --editor-selected-border: #007bff;
    --editor-shadow: rgba(0,0,0,0.1);
  }
  
  [data-theme="dark"] {
    --editor-bg: #1a1a1a;
    --editor-panel-bg: #2d2d2d;
    --editor-panel-input: #2d2d2d;
    --editor-panel-input-placeholder: #646464;
    --editor-border: #404040;
    --editor-text: #ffffff;
    --editor-text-muted: #a0a0a0;
    --editor-hover-bg: #1e1e1e;
    --editor-selected-bg: #1e3a5f;
    --editor-selected-border: #4a9eff;
    --editor-shadow: rgba(0,0,0,0.3);
  }
  
  #visual-page-editor-container {
    display: flex;
    height: 600px; /* Will be calculated dynamically by JavaScript */
    min-height: 300px;
  }
  
  #editor-titlebar {
    background: var(--editor-panel-bg);
    border-bottom: 1px solid var(--editor-border);
    padding: 10px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 15px;
    transition: background 0.3s ease, border-color 0.3s ease;
  }

  .titlebar-left {
    display: flex;
    align-items: center;
    gap: 15px;
  }

  .titlebar-left img {
    height: 32px;
    width: auto;
  }

  .titlebar-left h2 {
    margin: 0;
    font-size: 20px;
    color: var(--editor-text);
    font-weight: 600;
  }

  .titlebar-right {
    display: flex;
    gap: 10px;
  }
  
  #editor-toolbar {
    background: var(--editor-panel-bg);
    border-bottom: 1px solid var(--editor-border);
    padding: 10px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 20px;
    transition: background 0.3s ease, border-color 0.3s ease;
  }

  .toolbar-section.left {
    display: flex;
    justify-content: flex-start;
    gap: 5px;
  }

  .toolbar-section.center {
    display: flex;
    justify-content: start;
    flex-grow: 1;
    gap: 5px;
  }

  .toolbar-section.right {
    display: flex;
    justify-content: flex-end;
    gap: 5px;
  }
  
  #widget-palette {
    width: 250px;
    background: var(--editor-panel-bg);
    border-right: 1px solid var(--editor-border);
    overflow-y: auto;
    padding: 15px;
    transition: background 0.3s ease, border-color 0.3s ease;
  }
  
  #editor-canvas {
    flex: 1;
    background: var(--editor-bg);
    overflow-y: auto;
    padding: 20px;
    position: relative;
    transition: background 0.3s ease;
  }
  
  #properties-panel {
    width: 300px;
    background: var(--editor-panel-bg);
    border-left: 1px solid var(--editor-border);
    color: var(--editor-text);
    overflow-y: hidden;
    position: relative;
    transition: background 0.3s ease, border-color 0.3s ease;
    min-width: 200px;
    max-width: 600px;
    display: flex;
    flex-direction: column;
  }
  
  /* Properties Panel Resize Handle */
  #properties-panel-resize-handle {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    cursor: col-resize;
    background: transparent;
    z-index: 10;
    transition: background 0.2s;
  }
  
  #properties-panel-resize-handle:hover {
    background: var(--editor-selected-border);
  }
  
  #properties-panel-resize-handle.resizing {
    background: var(--editor-selected-border);
  }
  
  .widget-palette-item {
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    color: var(--editor-text);
    padding: 10px;
    margin-bottom: 10px;
    cursor: move;
    transition: all 0.2s;
  }
  
  .widget-palette-item:hover {
    box-shadow: 0 2px 8px var(--editor-shadow);
    border-color: var(--editor-selected-border);
  }
  
  .widget-palette-category {
    font-weight: bold;
    font-size: 14px;
    color: var(--editor-text);
    margin: 15px 0 10px 0;
    padding-bottom: 5px;
    border-bottom: 2px solid var(--editor-border);
  }
  
  /* Foundation Grid Integration for Editor */
  #editor-canvas .grid-x {
    display: flex;
    flex-flow: row wrap;
    margin-left: -0.625rem;
    margin-right: -0.625rem;
  }
  
  #editor-canvas .grid-x > .cell {
    flex: 0 0 auto;
    min-height: 0px;
    min-width: 0px;
    width: 100%;
    padding-left: 0.625rem;
    padding-right: 0.625rem;
    box-sizing: border-box;
  }
  
  /* Override canvas-column styles to work with Foundation grid */
  #editor-canvas .canvas-column {
    /* Remove conflicting styles */
    padding: 10px 0; /* Remove left/right padding, keep top/bottom */
    margin: 0; /* Remove any margins */
    box-sizing: border-box;
    position: relative;
    min-height: 60px;
  }
  
  /* Add inner content wrapper for proper spacing */
  #editor-canvas .column-content {
    position: relative;
    border-radius: 4px;
  }
  
  #editor-canvas .canvas-column > *:not(.column-resize-handle):not(.column-controls) {
    /* Content spacing handled by column-content wrapper */
  }
  
  /* Foundation responsive classes for editor */
  #editor-canvas .cell.small-1 { flex: 0 0 8.33333%; max-width: 8.33333%; }
  #editor-canvas .cell.small-2 { flex: 0 0 16.66667%; max-width: 16.66667%; }
  #editor-canvas .cell.small-3 { flex: 0 0 25%; max-width: 25%; }
  #editor-canvas .cell.small-4 { flex: 0 0 33.33333%; max-width: 33.33333%; }
  #editor-canvas .cell.small-5 { flex: 0 0 41.66667%; max-width: 41.66667%; }
  #editor-canvas .cell.small-6 { flex: 0 0 50%; max-width: 50%; }
  #editor-canvas .cell.small-7 { flex: 0 0 58.33333%; max-width: 58.33333%; }
  #editor-canvas .cell.small-8 { flex: 0 0 66.66667%; max-width: 66.66667%; }
  #editor-canvas .cell.small-9 { flex: 0 0 75%; max-width: 75%; }
  #editor-canvas .cell.small-10 { flex: 0 0 83.33333%; max-width: 83.33333%; }
  #editor-canvas .cell.small-11 { flex: 0 0 91.66667%; max-width: 91.66667%; }
  #editor-canvas .cell.small-12 { flex: 0 0 100%; max-width: 100%; }
  
  /* Medium viewport classes - only apply when canvas is medium or larger */
  #editor-canvas.viewport-medium .cell.medium-1,
  #editor-canvas.viewport-large .cell.medium-1 { flex: 0 0 8.33333%; max-width: 8.33333%; }
  #editor-canvas.viewport-medium .cell.medium-2,
  #editor-canvas.viewport-large .cell.medium-2 { flex: 0 0 16.66667%; max-width: 16.66667%; }
  #editor-canvas.viewport-medium .cell.medium-3,
  #editor-canvas.viewport-large .cell.medium-3 { flex: 0 0 25%; max-width: 25%; }
  #editor-canvas.viewport-medium .cell.medium-4,
  #editor-canvas.viewport-large .cell.medium-4 { flex: 0 0 33.33333%; max-width: 33.33333%; }
  #editor-canvas.viewport-medium .cell.medium-5,
  #editor-canvas.viewport-large .cell.medium-5 { flex: 0 0 41.66667%; max-width: 41.66667%; }
  #editor-canvas.viewport-medium .cell.medium-6,
  #editor-canvas.viewport-large .cell.medium-6 { flex: 0 0 50%; max-width: 50%; }
  #editor-canvas.viewport-medium .cell.medium-7,
  #editor-canvas.viewport-large .cell.medium-7 { flex: 0 0 58.33333%; max-width: 58.33333%; }
  #editor-canvas.viewport-medium .cell.medium-8,
  #editor-canvas.viewport-large .cell.medium-8 { flex: 0 0 66.66667%; max-width: 66.66667%; }
  #editor-canvas.viewport-medium .cell.medium-9,
  #editor-canvas.viewport-large .cell.medium-9 { flex: 0 0 75%; max-width: 75%; }
  #editor-canvas.viewport-medium .cell.medium-10,
  #editor-canvas.viewport-large .cell.medium-10 { flex: 0 0 83.33333%; max-width: 83.33333%; }
  #editor-canvas.viewport-medium .cell.medium-11,
  #editor-canvas.viewport-large .cell.medium-11 { flex: 0 0 91.66667%; max-width: 91.66667%; }
  #editor-canvas.viewport-medium .cell.medium-12,
  #editor-canvas.viewport-large .cell.medium-12 { flex: 0 0 100%; max-width: 100%; }
  
  /* Large viewport classes - only apply when canvas is large */
  #editor-canvas.viewport-large .cell.large-1 { flex: 0 0 8.33333%; max-width: 8.33333%; }
  #editor-canvas.viewport-large .cell.large-2 { flex: 0 0 16.66667%; max-width: 16.66667%; }
  #editor-canvas.viewport-large .cell.large-3 { flex: 0 0 25%; max-width: 25%; }
  #editor-canvas.viewport-large .cell.large-4 { flex: 0 0 33.33333%; max-width: 33.33333%; }
  #editor-canvas.viewport-large .cell.large-5 { flex: 0 0 41.66667%; max-width: 41.66667%; }
  #editor-canvas.viewport-large .cell.large-6 { flex: 0 0 50%; max-width: 50%; }
  #editor-canvas.viewport-large .cell.large-7 { flex: 0 0 58.33333%; max-width: 58.33333%; }
  #editor-canvas.viewport-large .cell.large-8 { flex: 0 0 66.66667%; max-width: 66.66667%; }
  #editor-canvas.viewport-large .cell.large-9 { flex: 0 0 75%; max-width: 75%; }
  #editor-canvas.viewport-large .cell.large-10 { flex: 0 0 83.33333%; max-width: 83.33333%; }
  #editor-canvas.viewport-large .cell.large-11 { flex: 0 0 91.66667%; max-width: 91.66667%; }
  #editor-canvas.viewport-large .cell.large-12 { flex: 0 0 100%; max-width: 100%; }

  .canvas-row {
    border: 2px dashed var(--editor-border);
    padding: 25px;
    margin-bottom: 15px;
    min-height: 80px;
    position: relative;
    cursor: pointer;
    transition: all 0.2s;
    background: var(--editor-bg);
  }
  
  .canvas-row:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
  }
  
  .canvas-row.selected {
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
    background: var(--editor-selected-bg);
  }
  
  .canvas-row.drag-over {
    background: var(--editor-selected-bg);
    border-color: var(--editor-selected-border);
  }
  
  .canvas-column {
    /* Foundation grid integration - minimal conflicting styles */
    position: relative;
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    min-height: 60px;
    /* Padding and sizing handled by Foundation grid classes above */
  }
  
  .canvas-column:hover {
    border-color: var(--editor-selected-border);
    box-shadow: 0 2px 8px var(--editor-shadow);
    background: var(--editor-hover-bg);
  }
  
  .canvas-column.selected {
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
    background: var(--editor-selected-bg);
  }
  
  .canvas-column.drag-over {
    background: var(--editor-selected-bg);
    border-color: var(--editor-selected-border);
  }
  
  .canvas-widget {
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    color: var(--editor-text);
    padding: 15px;
    margin-bottom: 10px;
    position: relative;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .canvas-widget:hover {
    border-color: var(--editor-selected-border);
    box-shadow: 0 2px 8px var(--editor-shadow);
  }
  
  .canvas-widget.selected {
    background: var(--editor-selected-bg);
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
  }
  
  .widget-controls {
    position: absolute;
    top: 10px;
    right: 5px;
    display: none;
  }
  
  .canvas-widget:hover .widget-controls {
    display: block;
  }

  .widget-drag-placeholder {
    background: #e9f4ff;
    border: 2px dashed #007bff;
    height: 50px;
    margin-bottom: 10px;
    transition: all 0.2s;
  }
  
  .row-drag-placeholder {
    background: #e9f4ff;
    border: 2px dashed #007bff;
    height: 80px;
    margin-bottom: 15px;
    transition: all 0.2s;
  }
  
  /* Enhanced column dragging styles */
  .canvas-column.dragging {
    opacity: 0.5;
    transform: rotate(2deg);
    z-index: 1000;
  }
  
  .canvas-column:hover {
    cursor: grab;
  }
  
  .canvas-column:active {
    cursor: grabbing;
  }
  
  /* Improved resize handle visibility */
  .column-resize-handle {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 6px;
    cursor: col-resize;
    background: transparent;
    z-index: 100;
    transition: background 0.2s;
    user-select: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
  }
  
  .column-resize-handle.left {
    left: -3px;
  }
  
  .column-resize-handle.right {
    right: -3px;
  }
  
  .column-resize-handle:hover,
  .column-resize-handle.resizing {
    background: var(--editor-selected-border);
    width: 6px;
  }
  
  .column-resize-handle.resizing {
    background: var(--editor-selected-border);
    opacity: 0.8;
  }
  
  /* Show resize handles on column hover */
  .canvas-column:hover .column-resize-handle {
    background: rgba(74, 158, 255, 0.3);
  }
  
  .row-controls {
    position: absolute;
    top: 0;
    right: 5px;
    display: none;
  }
  
  .canvas-row:hover .row-controls {
    display: block;
    z-index: 1;
  }
  
  .column-controls {
    position: absolute;
    top: 3px;
    right: 5px;
    display: none;
  }
  
  .canvas-column:hover .column-controls {
    display: block;
    z-index: 1;
  }
  
  .control-btn {
    background: #007bff;
    color: white;
    border: none;
    border-radius: 3px;
    padding: 4px 8px;
    margin-left: 4px;
    cursor: pointer;
    font-size: 12px;
  }
  
  .control-btn:hover {
    background: #0056b3;
  }
  
  .control-btn.danger {
    background: #dc3545;
  }
  
  .control-btn.danger:hover {
    background: #c82333;
  }
  
  .empty-canvas {
    text-align: center;
    padding: 60px 20px;
    color: var(--editor-text-muted);
  }
  
  .property-group {
    margin-bottom: 10px;
  }

  .property-group label {
    color: var(--editor-text);
  }
  
  /* Applied Classes Preview Box */
  #row-css-preview,
  #column-css-preview {
    background: var(--editor-panel-bg) !important;
    color: var(--editor-text) !important;
    border: 1px solid var(--editor-border);
  }
  
  [data-theme="dark"] #row-css-preview,
  [data-theme="dark"] #column-css-preview {
    background: #1e1e1e !important;
    color: #e0e0e0 !important;
  }
  
  .property-label {
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 5px;
    color: var(--editor-text);
  }
  
  .property-input {
    width: 100%;
    padding: 8px 8px 0 8px;
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    font-size: 14px;
    background: var(--editor-bg);
    color: var(--editor-text);
    transition: all 0.2s;
  }

  .property-input::placeholder { 
    color: var(--editor-panel-input-placeholder);
  }
  
  .property-input:focus {
    outline: none;
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 2px rgba(74, 158, 255, 0.25);
    background-color: var(--editor-panel-input)
  }
  
  .property-input.error {
    border-color: #dc3545 !important;
    box-shadow: 0 0 0 2px rgba(220, 53, 69, 0.25) !important;
  }
  
  select.property-input {
    background: var(--editor-bg);
    color: var(--editor-text);
  }
  
  /* Range Input Styles with Dark Mode Support */
  input[type="range"].property-input {
    padding: 8px 0;
    background: transparent;
    cursor: pointer;
    -webkit-appearance: none;
    appearance: none;
  }
  
  /* Range Track */
  input[type="range"].property-input::-webkit-slider-runnable-track {
    width: 100%;
    height: 6px;
    background: var(--editor-border);
    border-radius: 3px;
    transition: background 0.2s;
  }
  
  input[type="range"].property-input::-moz-range-track {
    width: 100%;
    height: 6px;
    background: var(--editor-border);
    border-radius: 3px;
    transition: background 0.2s;
  }
  
  /* Range Thumb */
  input[type="range"].property-input::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 18px;
    height: 18px;
    background: var(--editor-selected-border);
    border: 2px solid var(--editor-bg);
    border-radius: 50%;
    cursor: pointer;
    margin-top: -6px;
    box-shadow: 0 1px 3px var(--editor-shadow);
    transition: all 0.2s;
  }
  
  input[type="range"].property-input::-moz-range-thumb {
    width: 18px;
    height: 18px;
    background: var(--editor-selected-border);
    border: 2px solid var(--editor-bg);
    border-radius: 50%;
    cursor: pointer;
    box-shadow: 0 1px 3px var(--editor-shadow);
    transition: all 0.2s;
  }
  
  /* Hover States */
  input[type="range"].property-input:hover::-webkit-slider-thumb {
    background: #0056b3;
    transform: scale(1.1);
  }
  
  input[type="range"].property-input:hover::-moz-range-thumb {
    background: #0056b3;
    transform: scale(1.1);
  }
  
  input[type="range"].property-input:hover::-webkit-slider-runnable-track {
    background: var(--editor-selected-border);
  }
  
  input[type="range"].property-input:hover::-moz-range-track {
    background: var(--editor-selected-border);
  }
  
  /* Focus States */
  input[type="range"].property-input:focus {
    outline: none;
    box-shadow: none;
  }
  
  input[type="range"].property-input:focus::-webkit-slider-thumb {
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
  }
  
  input[type="range"].property-input:focus::-moz-range-thumb {
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
  }
  
  /* Dark Mode Specific Adjustments */
  [data-theme="dark"] input[type="range"].property-input::-webkit-slider-thumb {
    border-color: var(--editor-panel-bg);
  }
  
  [data-theme="dark"] input[type="range"].property-input::-moz-range-thumb {
    border-color: var(--editor-panel-bg);
  }
  
  /* TinyMCE HTML Editor Styles */
  .tinymce-editor-container {
    margin-bottom: 10px;
  }
  
  .html-editor-field {
    display: none; /* Hide the textarea when TinyMCE is active */
  }
  
  /* TinyMCE editor styling for dark mode compatibility */
  .tox .tox-toolbar,
  .tox .tox-toolbar__primary,
  .tox .tox-menubar {
    /* background: var(--editor-panel-bg) !important; */
  }
  
  .tox .tox-edit-area__iframe {
    /* background: var(--editor-bg) !important; */
  }

  /* Column Layout Picker Modal */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.5);
    display: none;
    z-index: 10000;
    justify-content: center;
    align-items: center;
  }

  .modal-overlay.active {
    display: flex !important;
  }

  .modal-content {
    background: var(--editor-bg);
    padding: 30px 30px 10px 30px;
    border-radius: 8px;
    box-shadow: 0 5px 15px var(--editor-shadow);
    width: 90%;
    max-width: 600px;
    max-height: 90vh;
    color: var(--editor-text);
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .layout-picker {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 20px;
  }

  .layout-option {
    border: 2px solid var(--editor-border);
    border-radius: 4px;
    padding: 15px;
    cursor: pointer;
    transition: all 0.2s;
    text-align: center;
    background: var(--editor-bg);
  }
  
  .layout-option:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
  }

  .layout-preview {
    display: flex;
    gap: 5px;
    height: 40px;
    margin-bottom: 10px;
  }

  .layout-preview-col {
    background: #ced4da;
    border-radius: 2px;
    flex-grow: 1;
  }

  .layout-palette-item {
    position: relative; /* Needed for positioning the button */
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    padding: 10px;
    margin-bottom: 10px;
    cursor: move;
    transition: all 0.2s;
  }

  .add-layout-btn {
    position: absolute;
    top: 5px;
    right: 5px;
    background: #007bff;
    color: white;
    border: none;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    font-size: 16px;
    line-height: 24px;
    text-align: center;
    cursor: pointer;
    display: none; /* Hidden by default */
    z-index: 2;
  }

  .layout-palette-item:hover .add-layout-btn {
    display: block; /* Show on hover */
  }

  .add-layout-btn:hover {
    background: #0056b3;
  }

  .layout-palette-item:hover {
    box-shadow: 0 2px 8px var(--editor-shadow);
    border-color: var(--editor-selected-border);
  }
  
  .layout-label {
    font-size: 11px;
    color: var(--editor-text-muted);
    text-align: center;
  }

  /* Pre-Designed Page Template Styles */
  #pre-designed-page-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  #pre-designed-page-list li {
    display: block;
  }

  #pre-designed-page-list a {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 15px;
    border: 2px solid var(--editor-border);
    border-radius: 6px;
    text-decoration: none;
    color: var(--editor-text);
    background: var(--editor-bg);
    cursor: pointer;
    transition: all 0.2s;
    height: 100%;
    min-height: 180px;
  }
  
  #pre-designed-page-list a:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
    box-shadow: 0 2px 8px var(--editor-shadow);
  }

  .template-preview {
    width: 100%;
    height: 80px;
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 3px;
    margin-bottom: 10px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .template-preview-row {
    display: flex;
    gap: 2px;
    height: 8px;
    flex-shrink: 0;
    min-height: 8px;
  }

  .template-preview-col {
    background: #ced4da;
    border-radius: 1px;
    flex: 1;
  }

  .template-label {
    font-size: 12px;
    color: var(--editor-text);
    font-weight: 500;
    text-align: center;
    word-wrap: break-word;
  }

  .palette-section {
    margin-bottom: 10px;
  }

  .palette-section-header {
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    user-select: none; /* Prevent text selection on click */
  }

  .palette-section-header:hover {
    color: #007bff;
  }

  .palette-section-header .toggle-icon {
    transition: transform 0.2s;
    font-size: 12px;
  }

  .palette-section-header.collapsed .toggle-icon {
    transform: rotate(-90deg);
  }

  .palette-section-content {
    padding-top: 10px;
  }

  /* Palette Tabs */
  #widget-palette {
    padding: 0; /* Remove padding to allow tabs to span full width */
  }

  .palette-tabs-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .tabs-nav {
    display: flex;
    list-style: none;
    margin: 0;
    padding: 0;
    border-bottom: 1px solid var(--editor-border);
    background-color: var(--editor-panel-bg);
    flex-shrink: 0;
  }

  .tabs-nav li {
    flex-grow: 1;
    text-align: center;
  }

  .tabs-nav a {
    display: block;
    padding: 8px 10px;
    text-decoration: none;
    color: var(--editor-text);
    border-bottom: 3px solid transparent;
    transition: all 0.2s;
    font-weight: 600;
    font-size: 12px;
    white-space: nowrap;
  }
  
  .tabs-nav a:hover {
    background-color: var(--editor-hover-bg);
  }
  
  .tabs-nav a.active {
    color: var(--editor-selected-border);
    border-bottom-color: var(--editor-selected-border);
  }

  /* Right Panel Tabs Styles */
  .right-panel-tabs-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .right-panel-tabs-nav {
    flex-shrink: 0;
    margin: 0 -15px;
    padding: 0 15px;
  }

  .right-panel-tabs-nav li {
    flex: 1;
    min-width: 0;
  }

  .right-panel-tabs-nav a {
    padding: 8px 6px;
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .right-panel-tab-content {
    flex-grow: 1;
    overflow-y: auto;
    padding: 15px 0;
  }

  .right-panel-tab-content h6 {
    color: var(--editor-text);
    margin-bottom: 10px;
  }

  .tab-content {
    display: none;
    padding: 15px;
    overflow-y: auto;
    flex-grow: 1;
  }

  .tab-content.active {
    display: block;
  }

  /* Pages Tab Styles */
  #web-page-list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  #web-page-list li {
    margin-bottom: 10px;
  }

  .web-page-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    background: var(--editor-bg);
  }
  
  .web-page-item:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
    box-shadow: 0 2px 8px var(--editor-shadow);
  }
  
  .web-page-item.selected {
    border-color: var(--editor-selected-border);
    background: var(--editor-selected-bg);
  }
  
  .web-page-item.new-page-item {
    border: 2px dashed var(--editor-selected-border);
    background: rgba(74, 158, 255, 0.05);
  }
  
  .web-page-item.new-page-item .web-page-title {
    color: var(--editor-selected-border);
    font-weight: 600;
  }
  
  .web-page-info {
    flex-grow: 1;
  }
  
  .web-page-title {
    font-weight: 600;
    color: var(--editor-text);
    margin-bottom: 3px;
  }
  
  .web-page-link {
    font-size: 12px;
    color: var(--editor-text-muted);
  }
  
  #pages-loading {
    text-align: center;
    padding: 20px;
    color: var(--editor-text-muted);
  }

  #pages-error {
    padding: 15px;
    background: #f8d7da;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
    color: #721c24;
    margin-bottom: 15px;
  }

  #pages-empty {
    text-align: center;
    padding: 40px 20px;
    color: var(--editor-text-muted);
  }
  
  /* Viewport Controls */
  .viewport-controls {
    display: flex;
    gap: 5px;
    align-items: center;
    margin-left: 15px;
  }

  .viewport-btn {
    transition: all 0.2s ease;
    border: 1px solid var(--editor-border);
    background: var(--editor-bg);
    color: var(--editor-text);
  }

  .viewport-btn:hover {
    background: var(--editor-hover-bg);
    border-color: var(--editor-selected-border);
  }

  .viewport-btn.active {
    background: var(--editor-selected-border);
    color: white;
    border-color: var(--editor-selected-border);
  }

  /* Viewport-specific canvas styles */
  #editor-canvas.viewport-small {
    transition: max-width 0.3s ease, margin 0.3s ease;
  }

  #editor-canvas.viewport-medium {
    transition: max-width 0.3s ease, margin 0.3s ease;
  }

  #editor-canvas.viewport-large {
    transition: max-width 0.3s ease, margin 0.3s ease;
  }

  /* Preview container viewport styles */
  #preview-container.viewport-small {
    transition: max-width 0.3s ease, margin 0.3s ease;
  }

  #preview-container.viewport-medium {
    transition: max-width 0.3s ease, margin 0.3s ease;
  }

  #preview-container.viewport-large {
    transition: max-width 0.3s ease, margin 0.3s ease;
  }

  #preview-iframe {
    transition: width 0.3s ease, max-width 0.3s ease;
    margin: 0 auto;
    display: block;
  }

  /* Viewport indicator */
  .viewport-indicator {
    position: absolute;
    top: 10px;
    right: 10px;
    background: var(--editor-selected-border);
    color: white;
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    z-index: 100;
    opacity: 0.8;
  }

  /* Dark Mode Toggle */
  #dark-mode-toggle {
    border: 1px solid var(--editor-border);
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 14px;
  }
  
  /* Preview Toggle */
  #preview-container {
    display: none;
    flex: 1;
    background: var(--editor-bg);
    overflow-y: auto;
    padding: 20px;
    position: relative;
  }

  #preview-container.active {
    display: block !important;
  }

  #editor-canvas.hidden {
    display: none;
  }

  #preview-loading {
    text-align: center;
    padding: 60px 20px;
    color: var(--editor-text-muted);
  }
  
  #preview-error {
    padding: 20px;
    background: rgba(220, 53, 69, 0.1);
    border: 1px solid rgba(220, 53, 69, 0.3);
    color: #dc3545;
    border-radius: 4px;
  }

  #preview-iframe {
    width: 100%;
    height: 100%;
    border: none;
    display: none;
    background-color: #ffffff;
  }

  #preview-iframe.active {
    display: block !important;
  }

  #preview-loading {
    text-align: center;
    padding: 60px 20px;
    color: #6c757d;
  }

  #preview-error {
    padding: 20px;
    background: #f8d7da;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
    color: #721c24;
    margin: 20px;
  }

  /* Toggle Switch Styles */
  #toggle-preview-btn {
    /* border: 2px solid #dee2e6; */
    /* padding: 6px 12px; */
    /* font-size: 13px; */
    /* font-weight: 600; */
    transition: all 0.3s ease;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    position: relative;
  }

  #toggle-preview-btn::after {
    content: '';
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #dee2e6;
    transition: all 0.3s ease;
  }

  #toggle-preview-btn.active {
    color: #ffffff;
    background-color: #43AC6A;
  }

  #toggle-preview-btn.active::after {
    background: #0dd757;
    box-shadow: 0 0 8px rgba(23, 162, 184, 0.5);
  }

  /* Floating Loading Indicator Overlay */
  .loading-indicator-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    backdrop-filter: blur(2px);
    transition: all 0.3s ease;
  }

  [data-theme="dark"] .loading-indicator-overlay {
    background: rgba(26, 26, 26, 0.9);
  }

  .loading-indicator-content {
    display: flex;
    align-items: center;
    gap: 12px;
    color: var(--editor-text);
    font-size: 16px;
    font-weight: 500;
    padding: 20px 30px;
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 8px;
    box-shadow: 0 4px 20px var(--editor-shadow);
    backdrop-filter: blur(10px);
  }

  .loading-indicator-content i {
    color: #007bff;
    font-size: 18px;
  }  

  [data-theme="dark"] #preview-loading-indicator {
    color: #ffffff;
  }
</style>
</g:compress>

<g:compress>
  <link href="${ctx}/css/platform.css" rel="stylesheet">
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/platform/preview-hover.css" />
</g:compress>
<g:compress>
  <script src="<c:url value='/javascript/widgets/editor/element-detector.js'/>"></script>
  <script src="<c:url value='/javascript/widgets/editor/hover-overlay.js'/>"></script>
  <script src="<c:url value='/javascript/widgets/editor/property-editor-bridge.js'/>"></script>
  <script src="<c:url value='/javascript/widgets/editor/preview-hover-manager.js'/>"></script>
</g:compress>
<web:script package="dragula" file="dragula.min.js" />

<div id="visual-page-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Title Bar -->
  <div id="editor-titlebar">
    <div class="titlebar-left">
      <img src="${ctx}/images/favicon.png" alt="Logo" />
      <h2>Webpage Editor</h2>
    </div>
    <div class="titlebar-right">
      <a href="${ctx}/admin/visual-page-editor" class="button tiny no-gap radius confirm-exit">Pages</a>
       <!-- <a href="${ctx}/admin/visual-page-editor" class="button tiny no-gap radius confirm-exit">Images</a> -->
       <!-- <a href="${ctx}/admin/visual-page-editor" class="button tiny no-gap radius confirm-exit">Content</a> -->
       <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="button tiny no-gap radius confirm-exit">Exit</a>
        </c:when>
        <c:when test="${!empty webPage.link}">
          <a href="${ctx}${webPage.link}" class="button tiny no-gap radius confirm-exit">Exit</a>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/" class="button tiny no-gap radius confirm-exit">Exit</a>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="add-page-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-file-circle-plus"></i> Add a Page</button>
      <button id="add-row-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-plus"></i> Add Row</button>
      <button id="undo-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-undo"></i> Undo</button>
      <button id="redo-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-redo"></i> Redo</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <button id="toggle-preview-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-eye"></i> Preview</button>
      <button id="save-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-save"></i> Publish</button>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="preview-loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
      <button id="dark-mode-toggle" class="button tiny secondary no-gap radius" title="Toggle Dark Mode"><i class="${font:far()} fa-moon"></i></button>
    </div>
  </div>
  
  <!-- Main Editor Container -->
  <div id="visual-page-editor-container">
    
    <!-- Widget Palette -->
    <div id="widget-palette">
      <div class="palette-tabs-container">
        <ul class="tabs-nav">
          <li><a href="#pages-tab" class="active">Pages</a></li>
          <li><a href="#layouts-tab">Layouts</a></li>
          <li><a href="#widgets-tab">Widgets</a></li>
        </ul>

        <div id="widgets-tab" class="tab-content">
          <input type="text" id="widget-search" placeholder="Search widgets..." class="property-input" style="margin-bottom: 15px;" />
          <div id="widget-list-container">
            <!-- Widgets will be dynamically inserted here -->
          </div>
        </div>

        <div id="layouts-tab" class="tab-content">
          <!-- Templates Button -->
          <div style="padding: 10px; border-bottom: 1px solid var(--editor-border);">
            <button id="pre-designed-page-btn-layouts" class="button expanded secondary radius" style="width: 100%; margin: 0;">
              <i class="${font:far()} fa-magic"></i> Choose a Template
            </button>
          </div>
          
          <div class="layout-palette-item" draggable="true" data-layout="small-12">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 100%;"></div>
            </div>
            <div class="layout-label">1 Column</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-6,small-6">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 50%;"></div>
              <div class="layout-preview-col" style="flex-basis: 50%;"></div>
            </div>
            <div class="layout-label">2 Columns</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-4,small-4,small-4">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
            </div>
            <div class="layout-label">3 Columns</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-3,small-3,small-3,small-3">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
            </div>
            <div class="layout-label">4 Columns</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-4,small-8">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
              <div class="layout-preview-col" style="flex-basis: 66.67%;"></div>
            </div>
            <div class="layout-label">33 / 67</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-8,small-4">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 66.67%;"></div>
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
            </div>
            <div class="layout-label">67 / 33</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-3,small-9">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 75%;"></div>
            </div>
            <div class="layout-label">25 / 75</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-9,small-3">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 75%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
            </div>
            <div class="layout-label">75 / 25</div>
          </div>
        </div>

        <div id="pages-tab" class="tab-content active">
          <div id="pages-loading" style="display: none;">
            <i class="${font:far()} fa-spinner fa-spin"></i> Loading pages...
          </div>
          <div id="pages-error" style="display: none;"></div>
          <div id="pages-empty" style="display: none;">
            <p>No pages available</p>
          </div>
          <ul id="web-page-list">
            <!-- Web pages will be dynamically inserted here -->
          </ul>
        </div>

      </div>
    </div>
    
    <!-- Preview Container -->
    <div id="preview-container">

      <!-- Floating Loading Indicator for Preview -->
      <!-- <div id="preview-loading-indicator" class="loading-indicator-overlay" style="display: none;">
        <div class="loading-indicator-content">
          <i class="${font:far()} fa-spinner fa-spin"></i> <span>Loading preview...</span>
        </div>
      </div> -->
      
      <!-- <div id="preview-loading" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i> Loading preview...
      </div> -->

      <div id="preview-error" style="display: none;"></div>
      <div style="height: 100%; width: 100%;<c:if test="${!empty themePropertyMap['theme.body.backgroundColor']}">background-color:<c:out value="${themePropertyMap['theme.body.backgroundColor']}" /></c:if>">
        <iframe id="preview-iframe" style="<c:if test="${!empty themePropertyMap['theme.body.backgroundColor']}">background-color:<c:out value="${themePropertyMap['theme.body.backgroundColor']}" /></c:if>"></iframe>
      </div>
    </div>
    
    <!-- Editor Canvas -->
    <div id="editor-canvas">


      <!-- Floating Loading Indicator -->
       <!--
      <div id="page-loading-indicator" class="loading-indicator-overlay" style="display: none;">
        <div class="loading-indicator-content">
          <i class="${font:far()} fa-spinner fa-spin"></i> <span id="loading-text">Loading...</span>
        </div>
      </div>
      -->
      
      <c:choose>
        <c:when test="${hasExistingLayout}">
          <!-- Existing layout will be rendered here via JavaScript -->
        </c:when>
        <c:otherwise>
          <div class="empty-canvas" style="cursor: pointer;">
            <i class="${font:far()} fa-plus-circle fa-3x margin-bottom-10"></i>
            <h5>Start Building Your Page</h5>
            <p>Click "Add Row" to begin or drag widgets from the palette</p>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
    
    <!-- Right Panel with Tabs -->
    <div id="properties-panel">
      <div id="properties-panel-resize-handle"></div>
      
      <!-- Right Panel Tabs -->
      <div class="right-panel-tabs-container">
        <ul class="tabs-nav right-panel-tabs-nav">
          <li><a href="#info-tab" class="active" data-tab="info">Page Info</a></li>
          <li><a href="#properties-tab" data-tab="properties">Properties</a></li>
          <li><a href="#css-tab" data-tab="css">CSS</a></li>
          <li><a href="#xml-tab" data-tab="xml">XML</a></li>
        </ul>
        
        <!-- Info Tab Content -->
        <div id="info-tab" class="tab-content right-panel-tab-content active" data-tab="info">
          <div id="info-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select a page to view its information</p>
          </div>
        </div>
        
        <!-- Properties Tab Content -->
        <div id="properties-tab" class="tab-content right-panel-tab-content" data-tab="properties">
          <div id="properties-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select an element to edit its properties</p>
          </div>
        </div>
        
        <!-- CSS Tab Content -->
        <div id="css-tab" class="tab-content right-panel-tab-content" data-tab="css">
          <div id="css-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">CSS editor will be loaded here</p>
          </div>
        </div>
        
        <!-- XML Tab Content -->
        <div id="xml-tab" class="tab-content right-panel-tab-content" data-tab="xml">
          <div id="xml-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">XML editor will be loaded here</p>
          </div>
        </div>
      </div>
    </div>
    
  </div>
</div>

<!-- Add Page Modal -->
<div id="add-page-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 500px;">
    <h4>New Page</h4>
    <form id="add-page-form">
      <div class="property-group">
        <label class="property-label" for="page-title">Title of Page</label>
        <input type="text" id="page-title" class="property-input" placeholder="Enter page title" required />
      </div>
      <div class="property-group">
        <label class="property-label" for="page-link">Link</label>
        <input type="text" id="page-link" class="property-input" placeholder="/page-url" required />
        <div id="page-link-error" style="color: #dc3545; font-size: 12px; margin-top: 5px; display: none;">
          Link must start with a forward slash (/)
        </div>
        <div style="font-size: 12px; color: #6c757d; margin-top: 5px;">
          The URL path for this page (e.g., /about, /contact, /products/new)
        </div>
      </div>
      <div style="text-align: right; margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end;">
        <button type="button" id="cancel-add-page" class="button tiny secondary radius">Cancel</button>
        <button type="submit" id="create-page-btn" class="button tiny success radius">Create Page</button>
      </div>
    </form>
  </div>
</div>

<!-- Pre-Designed Page Modal -->
<div id="pre-designed-page-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 800px;">
    <h4 style="color:var(--editor-text);margin: 0 0 15px 0; flex-shrink: 0;">Choose a Template</h4>
    <div id="icon-grid" style="flex: 1; overflow-y: auto; border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #f9f9f9; min-height: 200px;">
      <ul id="pre-designed-page-list">
        <!-- Templates items will be rendered here -->
      </ul>
    </div>
    <script>
      // Dynamically populate the pre-designed page list from preDesignedTemplates
      document.addEventListener('DOMContentLoaded', function() {
        var list = document.getElementById('pre-designed-page-list');
        if (window.preDesignedTemplates && window.preDesignedTemplateLabels) {
          // Use the order from window.preDesignedTemplateLabels keys
          Object.keys(window.preDesignedTemplateLabels).forEach(function(key) {
            console.log("Checking template key:", key);
            if (window.preDesignedTemplates[key]) {
              var label = window.preDesignedTemplateLabels[key] || key.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
              var li = document.createElement('li');
              var a = document.createElement('a');
              a.href = "#";
              a.setAttribute('data-template', key);
              
              // Create the template preview
              var preview = document.createElement('div');
              preview.className = 'template-preview';
              
              // Generate rows based on the template structure
              var template = window.preDesignedTemplates[key];
              if (Array.isArray(template) && template.length > 0) {
                // Show all rows in the preview
                for (var i = 0; i < template.length; i++) {
                  var row = template[i];
                  if (row.layout) {
                    var rowDiv = document.createElement('div');
                    rowDiv.className = 'template-preview-row';
                    
                    // Create columns based on layout
                    row.layout.forEach(function(colClass) {
                      var col = document.createElement('div');
                      col.className = 'template-preview-col';
                      var match = colClass.match(/small-(\d+)/);
                      if (match) {
                        var width = (parseInt(match[1]) / 12) * 100;
                        col.style.flex = width;
                      }
                      rowDiv.appendChild(col);
                    });
                    
                    preview.appendChild(rowDiv);
                  }
                }
              }
              
              // Create the label
              var labelDiv = document.createElement('div');
              labelDiv.className = 'template-label';
              labelDiv.textContent = label;
              
              a.appendChild(preview);
              a.appendChild(labelDiv);
              li.appendChild(a);
              list.appendChild(li);
            }
          });
        }
      });
    </script>
    <div style="text-align: right; margin-top: 15px; display: flex; gap: 10px; justify-content: flex-end; flex-shrink: 0;">
      <button id="close-pre-designed-page-modal" class="button tiny secondary radius">Cancel</button>

    <!-- Initialize Preview Hover integration -->
    <script>
      (function () {
        function initPreviewHover() {
          try {
            // Prefer preview iframe's document/body as the hover context
            var iframe = document.getElementById('preview-iframe');
            var initWithIframe = function(ifr) {
              if (!ifr || !ifr.contentDocument) { return null; }
              var iframeDoc = ifr.contentDocument;
              var container = iframeDoc.body || iframeDoc.documentElement;
              return { doc: iframeDoc, win: ifr.contentWindow, container: container };
            };

            var ctx = initWithIframe(iframe);
            var previewContainer = (ctx && ctx.container) ? ctx.container : (document.getElementById('editor-canvas') || document.querySelector('#visual-page-editor-wrapper'));
            if (!previewContainer) { return; }

            // Ensure hover CSS is available inside iframe
            if (ctx && ctx.doc) {
              var cssHref = '${ctx}/css/platform/preview-hover.css';
              var exists = ctx.doc.querySelector('link[href$="/css/platform/preview-hover.css"]');
              if (!exists) {
                try {
                  console.log('Preview hover: injecting CSS into iframe');
                  var link = ctx.doc.createElement('link');
                  link.rel = 'stylesheet';
                  link.href = cssHref;
                  (ctx.doc.head || ctx.doc.documentElement).appendChild(link);
                } catch (e) { /* ignore */ }
              }
            }

            var propertyApi = (window.PropertyEditorAPI || window.EditorPropertyAPI || null);
            var manager = new (window.PreviewHoverManager || function(){}) (previewContainer, propertyApi);

            // Expose for debugging
            window.__previewHoverManager = manager;

            // Set up iframe postMessage proxy for reliable event capture
            function setupIframeEventProxy() {
              if (!iframe || !iframe.contentWindow || !ctx || !ctx.doc) {
                console.warn('Preview hover: iframe proxy setup skipped - missing context');
                return;
              }
              
              // Inject event proxy script into iframe
              var proxyScript = ctx.doc.createElement('script');
              proxyScript.textContent = '(function() {' +
                'var throttle = 16;' +
                'var lastTime = 0;' +
                'var eventCount = 0;' +
                'function forwardEvent(e) {' +
                '  var now = Date.now();' +
                '  if (now - lastTime < throttle) return;' +
                '  lastTime = now;' +
                '  eventCount++;' +
                '  if (eventCount <= 3) {' +
                '    console.log("Iframe proxy: forwarding event", e.type, e.clientX, e.clientY);' +
                '  }' +
                '  if (window.parent && e.clientX != null && e.clientY != null) {' +
                '    try {' +
                '      window.parent.postMessage({' +
                '      type: "preview-hover-event",' +
                '      eventType: e.type,' +
                '      clientX: e.clientX,' +
                '      clientY: e.clientY,' +
                '      target: e.target ? (e.target.tagName || "unknown") : "unknown"' +
                '      }, "*");' +
                '    } catch (err) {' +
                '      console.error("Iframe proxy: postMessage failed", err);' +
                '    }' +
                '  }' +
                '}' +
                'document.addEventListener("mousemove", forwardEvent, true);' +
                'document.addEventListener("pointermove", forwardEvent, true);' +
                'console.log("Preview hover: iframe event proxy active");' +
              '})();';
              
              try {
                (ctx.doc.body || ctx.doc.documentElement).appendChild(proxyScript);
                console.log('Preview hover: iframe event proxy injected');
              } catch (e) {
                console.warn('Preview hover: failed to inject iframe proxy', e);
              }
              
              // Parent listener for postMessage events
              var messageCount = 0;
              window.addEventListener('message', function(e) {
                if (!e.data || e.data.type !== 'preview-hover-event') { return; }
                
                messageCount++;
                if (messageCount <= 3) {
                  console.log('Parent received hover event:', e.data.clientX, e.data.clientY);
                }
                
                // Forward to element detector if manager is active
                if (manager && manager.elementDetector && manager.isEnabled) {
                  var detector = manager.elementDetector;
                  if (detector && typeof detector.detectElementAtPoint === 'function') {
                    var elementInfo = detector.detectElementAtPoint(e.data.clientX, e.data.clientY);
                    if (elementInfo && detector.onElementDetected) {
                      detector.onElementDetected(elementInfo);
                    } else if (!elementInfo && detector.onElementLost) {
                      detector.onElementLost();
                    }
                  }
                } else {
                  if (messageCount === 1) {
                    console.warn('Preview hover: manager not active', {
                      hasManager: !!manager,
                      hasDetector: !!(manager && manager.elementDetector),
                      isEnabled: !!(manager && manager.isEnabled)
                    });
                  }
                }
              });
            }

            // Helper: determine current preview mode from wrapper
            function isPreviewMode() {
              var wrapper = document.getElementById('visual-page-editor-wrapper');
              if (!wrapper) return false;
              var modeAttr = wrapper.getAttribute('data-mode');
              if (modeAttr) return modeAttr === 'preview';
              return wrapper.classList.contains('preview') || wrapper.classList.contains('preview-mode');
            }

            // Initial state (ensure iframe-ready)
            if (iframe && iframe.contentDocument && iframe.contentDocument.readyState !== 'complete') {
              iframe.addEventListener('load', function(){
                setupIframeEventProxy();
                manager.handlePreviewModeChange(isPreviewMode());
              });
            } else {
              setupIframeEventProxy();
              manager.handlePreviewModeChange(isPreviewMode());
            }

            // Listen for existing editor mode-change events if available
            document.addEventListener('cms:editor:mode-change', function (e) {
              var mode = e && e.detail && e.detail.mode ? e.detail.mode : null;
              manager.handlePreviewModeChange(mode === 'preview');
            });

            // Fallback: observe class/attribute changes on wrapper
            var wrapper = document.getElementById('visual-page-editor-wrapper');
            if (wrapper && window.MutationObserver) {
              var observer = new MutationObserver(function () {
                manager.handlePreviewModeChange(isPreviewMode());
              });
              observer.observe(wrapper, { attributes: true, attributeFilter: ['class', 'data-mode'] });
            }

            // Clean up on unload
            window.addEventListener('beforeunload', function () {
              try { manager.disable(); } catch (e) {}
            });
          } catch (err) {
            if (window.console && console.error) {
              console.error('Preview Hover init failed:', err);
            }
          }
        }

        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', initPreviewHover);
        } else {
          initPreviewHover();
        }
      })();
    </script>
    </div>
  </div>
</div>

<!-- Hidden form for submission -->
<form id="editor-form" method="post" style="display: none;">
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <input type="hidden" name="webPage" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="webPageLink" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="returnPage" value="${returnPage}" />
  <input type="hidden" id="designer-data" name="designerData" value=""/>
</form>

<!-- Store existing XML safely for JS -->
<script id="existing-xml-data" type="text/plain"><c:out value="${webPage.pageXml}" escapeXml="true"/></script>

<!-- Load JavaScript modules -->
<web:script package="ace" file="ace.js" charset="utf-8" />
<web:script package="ace" file="mode-css.js" charset="utf-8" />
<web:script package="ace" file="mode-xml.js" charset="utf-8" />
<web:script package="ace" file="theme-chrome.js" charset="utf-8" />
<web:script package="ace" file="theme-monokai.js" charset="utf-8" />
<g:compress>
  <script src="${ctx}/javascript/icon-picker-modal.js"></script>
  <script src="${ctx}/javascript/page-link-picker-modal.js"></script>
  <script src="${ctx}/javascript/widgets/editor/widget-registry.js"></script>
  <script src="${ctx}/javascript/widgets/editor/pre-designed-templates.js"></script>
  <script src="${ctx}/javascript/widgets/editor/viewport-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/editor-main.js"></script>
  <script src="${ctx}/javascript/widgets/editor/drag-drop-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/layout-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/canvas-controller.js"></script>
  <script src="${ctx}/javascript/widgets/editor/properties-panel.js"></script>
  <script src="${ctx}/javascript/widgets/editor/pages-tab-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/right-panel-tabs.js"></script>
  <script src="${ctx}/javascript/widgets/editor/info-tab-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/css-tab-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/xml-tab-manager.js"></script>
</g:compress>

<script>
  // Initialize the editor
  // Declare propertiesPanel at module scope so it's accessible throughout
  let propertiesPanel;
  
  document.addEventListener('DOMContentLoaded', function() {
    const editorConfig = {
      webPageLink: '<c:out value="${webPage.link}" />',
      webPageId: <c:out value="${webPage.id}" default="-1"/>,
      existingXml: document.getElementById('existing-xml-data').textContent
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&#034;/g, '"')
        .replace(/&amp;/g, '&'),
      hasExistingLayout: <c:out value="${hasExistingLayout ? 'true' : 'false'}" default="false"/>
    };
    
    window.pageEditor = new PageEditor(editorConfig);
    
    // Initialize PreviewHoverManager for hover functionality in preview mode
    // const previewContainer = document.getElementById('preview-container');
    // const previewIframe = document.getElementById('preview-iframe');
    
    // Set up preview update listener BEFORE initializing the page editor
    let isPreviewMode = false;
    const togglePreviewBtn = document.getElementById('toggle-preview-btn');
    const previewContainer = document.getElementById('preview-container');
    const editorCanvas = document.getElementById('editor-canvas');
    const previewIframe = document.getElementById('preview-iframe');
    const previewLoading = document.getElementById('preview-loading');
    const previewError = document.getElementById('preview-error');

    // Guard: cleanup any lingering Dragula artifacts that may affect scrolling
    function cleanupDragulaArtifacts() {
      try {
        // Ensure iframe allows pointer interactions
        if (previewIframe) {
          previewIframe.style.pointerEvents = 'auto';
        }
        // Restore scrolling behavior in preview container
        if (previewContainer) {
          previewContainer.style.touchAction = 'auto';
          previewContainer.style.overscrollBehavior = 'auto';
        }
        // Remove global dragula class if it lingers
        if (document && document.body && document.body.classList) {
          document.body.classList.remove('gu-unselectable');
        }
        // Remove any orphaned mirror nodes
        Array.prototype.slice.call(document.querySelectorAll('.gu-mirror')).forEach(function(node){
          if (node && node.parentNode && typeof node.remove === 'function') { node.remove(); }
          else if (node && node.parentNode) { node.parentNode.removeChild(node); }
        });
      } catch (e) {
        console.debug('Preview cleanupDragulaArtifacts error:', e);
      }
    }

    // Get the actual PropertiesPanel instance for use by PreviewHoverManager
    propertiesPanel = window.pageEditor.getPropertiesPanel();
    
    // Initialize PreviewHoverManager with the preview iframe as the container
    // The iframe content will be the actual preview container where hover detection occurs
    window.previewHoverManager = new PreviewHoverManager(previewIframe, propertiesPanel);
    
    // General function to open a page in the preview iframe
    window.openPageInIframe = function(url, loadingMessage = 'Loading...') {
      // Show loading indicator
      if (window.pageEditor) {
        window.pageEditor.showLoadingIndicator(loadingMessage);
      }
      
      // Reset the properties panel
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      
      // Set iframe source
      previewIframe.src = url;
      
      // Handle iframe load
      previewIframe.onload = function() {
        // Hide loading indicator when iframe loads
        if (window.pageEditor) {
          window.pageEditor.hideLoadingIndicator();
        }
      };
      
      // Switch to preview mode
      previewIframe.classList.add('active');
      previewContainer.classList.add('active');
      editorCanvas.classList.add('hidden');
      togglePreviewBtn.classList.add('active');
      isPreviewMode = true;
      // Ensure scrolling remains functional after entering preview
      cleanupDragulaArtifacts();
    };

    // Function to refresh the preview
    function refreshPreview() {
      previewIframe.style.display = 'none';
      previewError.style.display = 'none';
      

      
      // Show floating loading indicator for preview
      const previewLoadingIndicator = document.getElementById('preview-loading-indicator');
      if (previewLoadingIndicator) {
        previewLoadingIndicator.style.display = 'block';
      }
      
      // Show loading indicator in toolbar
      if (window.pageEditor) {
        window.pageEditor.showLoadingIndicator('Loading preview...');
      }

      




      // Get the current editor data as XML
      const layoutManager = window.pageEditor.getLayoutManager();
      const designerData = layoutManager.toXML();

      // Get the selected page link from the pages tab manager
      const webPageLink = window.pageEditor.pagesTabManager.getSelectedPageLink();
      
      // Get current viewport for preview sizing
      const currentViewport = window.pageEditor.getViewportManager().getCurrentViewport();
      
      // Send to server for rendering
      const formData = new FormData();
      // formData.append('widget', '<c:out value="${widgetContext.uniqueId}" />');
      formData.append('token', '<c:out value="${userSession.formToken}" />');
      formData.append('webPageLink', webPageLink);
      formData.append('designerData', designerData);
      formData.append('containerPreview', 'true');
      formData.append('viewport', currentViewport); // Add viewport info
      
      fetch(webPageLink, {
        method: 'POST',
        body: formData
      })
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to load preview: ' + response.statusText);
        }
        return response.text();
      })
      .then(html => {
        // Hide loading indicators
        if (previewLoadingIndicator) {
          previewLoadingIndicator.style.display = 'none';
        }
        if (window.pageEditor) {
          window.pageEditor.hideLoadingIndicator();
        }
        // Write the complete HTML response to the iframe
        const iframeDoc = previewIframe.contentDocument || previewIframe.contentWindow.document;
        iframeDoc.open();
        iframeDoc.write(html);
        iframeDoc.close();
        
        // Added by server...
        // Inject the hover bridge script into the iframe document after content is written
        // Use a small timeout to ensure body exists
        // setTimeout(() => {
        //   const freshDoc = previewIframe.contentDocument || previewIframe.contentWindow.document;
        //   if (freshDoc && freshDoc.body) {
        //     const bridgeScript = freshDoc.createElement('script');
        //     bridgeScript.src = '${ctx}/javascript/widgets/editor/iframe-hover-bridge.js';
        //     freshDoc.body.appendChild(bridgeScript);
        //   }
        // }, 10);

        
        // Apply viewport styles to iframe after content loads
        previewIframe.onload = function() {
          if (window.pageEditor && window.pageEditor.getViewportManager()) {
            window.pageEditor.getViewportManager().applyPreviewViewportStyles();
          }
          
          // Re-initialize hover manager for the new iframe content
          if (window.previewHoverManager && isPreviewMode) {
            // Refresh iframe references after preview reload
            window.previewHoverManager.refreshIframeReferences();
            
            // Small delay to ensure iframe content is fully loaded
            setTimeout(() => {
              window.previewHoverManager.handlePreviewModeChange(true);
            }, 100);
          }
        };
        
        previewIframe.style.display = 'block';
        // Clean up any lingering drag state that could affect scrolling
        cleanupDragulaArtifacts();
      })
      .catch(error => {
        // Hide loading indicators
        if (previewLoadingIndicator) {
          previewLoadingIndicator.style.display = 'none';
        }
        if (window.pageEditor) {
          window.pageEditor.hideLoadingIndicator();
        }
        previewError.style.display = 'block';
        previewError.textContent = 'Error loading preview: ' + error.message;
      });
    }

    // Make refreshPreview globally available for property change events
    window.refreshPreview = refreshPreview;

    // Listen for page changes
    document.addEventListener('pageChanged', function(e) {
      console.log('pageChanged event fired, isPreviewMode:', isPreviewMode);
      // Reset the properties panel (unselect any previously selected widget)
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      
      // If in preview mode, refresh the preview when page is switched
      if (isPreviewMode) {
        console.log('Refreshing preview due to page change');
        // Add a small delay to ensure the layout is fully processed
        setTimeout(() => {
          refreshPreview();
        }, 100);
      }
    });

    // Listen for viewport changes
    document.addEventListener('viewportChanged', function(e) {
      console.log('viewportChanged event fired, isPreviewMode:', isPreviewMode);
      
      // If in preview mode, refresh the preview when viewport is switched
      if (isPreviewMode) {
        console.log('Refreshing preview due to viewport change');
        // Add a small delay to ensure the viewport styles are applied
        setTimeout(() => {
          refreshPreview();
        }, 100);
      }
    });

    togglePreviewBtn.addEventListener('click', function() {
      isPreviewMode = !isPreviewMode;
      
      if (isPreviewMode) {
        // Switch to preview mode
        editorCanvas.classList.add('hidden');
        previewContainer.classList.add('active');
        togglePreviewBtn.classList.add('active');
        
        // Enable preview hover functionality
        if (window.previewHoverManager) {
          window.previewHoverManager.handlePreviewModeChange(true);
        }
        
        // Load preview
        refreshPreview();
      } else {
        // Switch back to editor mode
        editorCanvas.classList.remove('hidden');
        previewContainer.classList.remove('active');
        togglePreviewBtn.classList.remove('active');
        
        // Disable preview hover functionality
        if (window.previewHoverManager) {
          window.previewHoverManager.handlePreviewModeChange(false);
        }
      }
    });

    // Add click handler to preview-container background to unselect elements
    // This allows users to click outside the iframe to clear all selections
    previewContainer.addEventListener('click', function(e) {
      // Only trigger if clicking directly on the preview-container background (not on child elements like iframe)
      if (e.target === previewContainer && window.previewHoverManager) {
        console.log('Preview container background clicked, clearing selections');
        // Disable and re-enable to clear the current outline
        window.previewHoverManager.disable();
        window.previewHoverManager.enable();
        // Clear properties panel
        if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
          window.pageEditor.getPropertiesPanel().clear();
        }
      }
    });

    // Now initialize the page editor
    window.pageEditor.init();

    // Enhancement: Add canvas background click handler to unselect everything
    if (editorCanvas) {
      editorCanvas.addEventListener('click', function(e) {
        // Only unselect if clicking directly on the canvas background (not on child elements)
        if (e.target === editorCanvas) {
          // Clear the properties panel (unselect everything)
          if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
            document.querySelectorAll('.canvas-widget.selected').forEach(el => {
              el.classList.remove('selected');
            });
            window.pageEditor.getPropertiesPanel().clear();
          }
        }
      });
    }

    // Set up middle section button handlers
    const returnPage = '<c:out value="${returnPage}" />';
    
    // Exit button confirmation handler
    const exitButtons = document.querySelectorAll('a.confirm-exit');
    exitButtons.forEach(button => {
      button.addEventListener('click', async function(e) {
      const isDirty = window.pageEditor.isDirty && window.pageEditor.isDirty();
      if (isDirty) {
        e.preventDefault();
        const confirmed = await window.pageEditor.showConfirmDialog('You have unsaved changes. Are you sure you want to exit?');
        if (confirmed) {
          window.location.href = button.href;
        }
      }
      });
    });

    // Add Page button handler
    document.getElementById('add-page-btn').addEventListener('click', async function(e) {
      e.preventDefault();
      
      // Check if editor has unsaved changes
      const isDirty = window.pageEditor.isDirty && window.pageEditor.isDirty();
      if (isDirty) {
        const confirmed = await window.pageEditor.showConfirmDialog('You have unsaved changes. Are you sure you want to create a new page?');
        if (!confirmed) {
          return;
        }
      }
      
      // Show the add page modal
      showAddPageModal();
    });
    
    // Helper function to close modal
    function closePreDesignedPageModal() {
      document.getElementById('pre-designed-page-modal').classList.remove('active');
    }

    // Show modal - handle both toolbar button (if exists) and layouts tab button
    const toolbarBtn = document.getElementById('pre-designed-page-btn');
    const layoutsBtn = document.getElementById('pre-designed-page-btn-layouts');
    
    if (toolbarBtn) {
      toolbarBtn.addEventListener('click', function() {
        document.getElementById('pre-designed-page-modal').classList.add('active');
      });
    }
    
    if (layoutsBtn) {
      layoutsBtn.addEventListener('click', function() {
        document.getElementById('pre-designed-page-modal').classList.add('active');
      });
    }
    
    // Hide modal
    document.getElementById('close-pre-designed-page-modal').addEventListener('click', function() {
      closePreDesignedPageModal();
    });
    
    // Handle template selection
    document.getElementById('pre-designed-page-list').addEventListener('click', function(e) {
      // Find the closest anchor tag to handle clicks on child elements
      const link = e.target.closest('a');
      if (link) {
        e.preventDefault();
        const templateKey = link.getAttribute('data-template');
        const template = preDesignedTemplates[templateKey];
        if (template && window.pageEditor) {
          const layoutManager = window.pageEditor.getLayoutManager();
          // Remove all existing rows
          layoutManager.structure.rows = [];
          // Add new rows/widgets from template
          template.forEach(row => {
            const rowId = layoutManager.addRow(row.layout);
            const rowObj = layoutManager.getRow(rowId);
            if (Array.isArray(row.widgets)) {
              row.widgets.forEach((widget, colIdx) => {
                if (rowObj && rowObj.columns[colIdx]) {
                  const widgetId = layoutManager.addWidget(rowId, rowObj.columns[colIdx].id, widget.type);
                  // Set widget properties
                  const widgetObj = layoutManager.getWidget(widgetId);
                  if (widgetObj && widget.properties) {
                    Object.keys(widget.properties).forEach(propKey => {
                      widgetObj.properties[propKey] = widget.properties[propKey];
                    });
                  }
                }
              });
            }
          });
          // Re-render the layout after adding all rows/widgets
          window.pageEditor.getCanvasController().renderLayout(layoutManager.getStructure());
          window.pageEditor.saveToHistory();
        }
        closePreDesignedPageModal();
      }
    });

    // Set up palette tabs (left panel only - scoped to widget-palette)
    const widgetPalette = document.getElementById('widget-palette');
    const paletteTabs = widgetPalette.querySelectorAll('.tabs-nav a');
    const paletteTabContents = widgetPalette.querySelectorAll('.tab-content');

    paletteTabs.forEach(tab => {
      tab.addEventListener('click', e => {
        e.preventDefault();

        paletteTabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        const target = document.querySelector(tab.getAttribute('href'));
        paletteTabContents.forEach(tc => tc.classList.remove('active'));
        target.classList.add('active');
      });
    });

    // Handle clicking the add layout button
    document.querySelectorAll('.add-layout-btn').forEach(button => {
      button.addEventListener('click', (e) => {
        e.stopPropagation(); // Prevent triggering drag-and-drop
        const layoutItem = e.target.closest('.layout-palette-item');
        const layout = layoutItem.dataset.layout.split(',');
        window.pageEditor.addRow(layout);
      });
    });

    // Calculate the container height dynamically
    function calculateContainerHeight() {
      const wrapper = document.getElementById('visual-page-editor-wrapper');
      const titlebar = document.getElementById('editor-titlebar');
      const toolbar = document.getElementById('editor-toolbar');
      const container = document.getElementById('visual-page-editor-container');
      
      if (!wrapper || !titlebar || !toolbar || !container) return;
      
      // Get the viewport height
      const viewportHeight = window.innerHeight;
      
      // Calculate the height used by elements above the container
      const wrapperTop = wrapper.getBoundingClientRect().top;
      const titlebarHeight = titlebar.offsetHeight;
      const toolbarHeight = toolbar.offsetHeight;
      
      // Get any title element if it exists
      const titleElement = wrapper.querySelector('h4');
      const titleHeight = titleElement ? titleElement.offsetHeight : 0;
      
      // Get the messages container if it exists
      const messagesElement = wrapper.querySelector('[role="alert"], .messages-container');
      const messagesHeight = messagesElement ? messagesElement.offsetHeight : 0;
      
      // Calculate available height: viewport height - space from top - title - messages - titlebar - toolbar
      const availableHeight = viewportHeight - wrapperTop - titleHeight - messagesHeight - titlebarHeight - toolbarHeight;
      
      // Set the container height with a minimum
      const finalHeight = Math.max(availableHeight, 300);
      container.style.height = finalHeight + 'px';
    }

    // Add Page Modal Functions
    function showAddPageModal() {
      const modal = document.getElementById('add-page-modal');
      const titleInput = document.getElementById('page-title');
      const linkInput = document.getElementById('page-link');
      const errorDiv = document.getElementById('page-link-error');
      
      // Clear previous values and errors
      titleInput.value = '';
      linkInput.value = '';
      errorDiv.style.display = 'none';
      linkInput.classList.remove('error');
      
      // Show modal
      modal.classList.add('active');
      modal.style.display = 'flex';
      
      // Focus on title input
      setTimeout(() => {
        titleInput.focus();
      }, 100);
    }

    function hideAddPageModal() {
      const modal = document.getElementById('add-page-modal');
      modal.classList.remove('active');
      modal.style.display = 'none';
    }

    // Make functions globally accessible
    window.showAddPageModal = showAddPageModal;
    window.hideAddPageModal = hideAddPageModal;
    window.createNewPage = createNewPage;

    function validatePageLink(link) {
      // Must start with /
      if (!link.startsWith('/')) {
        return false;
      }
      
      // Basic validation - no spaces, valid URL characters
      const validLinkPattern = /^\/[a-zA-Z0-9\-_\/]*$/;
      return validLinkPattern.test(link);
    }

    function generateLinkFromTitle(title) {
      if (!title) return '';
      
      // Convert title to URL-friendly format
      return '/' + title
        .toLowerCase()
        .replace(/[^a-z0-9\s\-_]/g, '') // Remove special characters
        .replace(/\s+/g, '-') // Replace spaces with hyphens
        .replace(/\-+/g, '-') // Replace multiple hyphens with single
        .replace(/^\-|\-$/g, ''); // Remove leading/trailing hyphens
    }

    // Set up Add Page Modal event listeners
    document.getElementById('cancel-add-page').addEventListener('click', hideAddPageModal);
    
    // Auto-generate link from title
    document.getElementById('page-title').addEventListener('input', function(e) {
      const linkInput = document.getElementById('page-link');
      if (!linkInput.value || linkInput.dataset.autoGenerated !== 'false') {
        const generatedLink = generateLinkFromTitle(e.target.value);
        linkInput.value = generatedLink;
        linkInput.dataset.autoGenerated = 'true';
      }
    });
    
    // Mark link as manually edited when user types in it
    document.getElementById('page-link').addEventListener('input', function(e) {
      e.target.dataset.autoGenerated = 'false';
      
      // Clear error state when user starts typing
      const errorDiv = document.getElementById('page-link-error');
      errorDiv.style.display = 'none';
      e.target.classList.remove('error');
    });
    
    // Handle form submission
    document.getElementById('add-page-form').addEventListener('submit', function(e) {
      e.preventDefault();
      
      const titleInput = document.getElementById('page-title');
      const linkInput = document.getElementById('page-link');
      const errorDiv = document.getElementById('page-link-error');
      const createBtn = document.getElementById('create-page-btn');
      
      const title = titleInput.value.trim();
      const link = linkInput.value.trim();
      
      // Validate inputs
      if (!title) {
        titleInput.focus();
        return;
      }
      
      if (!link) {
        linkInput.focus();
        return;
      }
      
      if (!validatePageLink(link)) {
        errorDiv.style.display = 'block';
        linkInput.classList.add('error');
        linkInput.style.borderColor = '#dc3545';
        linkInput.focus();
        return;
      }
      
      // Check if page already exists
      const existingPage = window.pageEditor.pagesTabManager.pages.find(page => page.link === link);
      if (existingPage) {
        errorDiv.textContent = 'A page with this link already exists';
        errorDiv.style.display = 'block';
        linkInput.classList.add('error');
        linkInput.style.borderColor = '#dc3545';
        linkInput.focus();
        return;
      }
      
      // Check if we're already editing a new page with this link
      if (window.pageEditor.pagesTabManager.selectedPageId === 'new' && 
          window.pageEditor.pagesTabManager.selectedPageLink === link) {
        errorDiv.textContent = 'You are already editing a page with this link';
        errorDiv.style.display = 'block';
        linkInput.classList.add('error');
        linkInput.style.borderColor = '#dc3545';
        linkInput.focus();
        return;
      }
      
      // Disable create button to prevent double submission
      createBtn.disabled = true;
      createBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Creating...';
      
      // Create the new page
      createNewPage(title, link);
    });
    
    function createNewPage(title, link) {
      console.log('Creating new page:', title, link);
      
      // Hide the modal
      hideAddPageModal();
      
      // Switch to the Pages tab to show the new page
      const pagesTab = document.querySelector('a[href="#pages-tab"]');
      const pagesTabContent = document.getElementById('pages-tab');
      
      if (pagesTab && pagesTabContent) {
        // Remove active class from all palette tabs (left panel only)
        const widgetPalette = document.getElementById('widget-palette');
        widgetPalette.querySelectorAll('.tabs-nav a').forEach(tab => tab.classList.remove('active'));
        widgetPalette.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        
        // Activate the Pages tab
        pagesTab.classList.add('active');
        pagesTabContent.classList.add('active');
        
        console.log('Switched to Pages tab');
      }
      
      // Use the PageEditor's createNewPage method
      if (window.pageEditor && typeof window.pageEditor.createNewPage === 'function') {
        window.pageEditor.createNewPage(title, link);
      } else {
        console.error('PageEditor createNewPage method not available');
      }
      
      // Re-enable create button for next time
      const createBtn = document.getElementById('create-page-btn');
      createBtn.disabled = false;
      createBtn.innerHTML = 'Create Page';
    }

    // Close modal on ESC key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        const addPageModal = document.getElementById('add-page-modal');
        if (addPageModal && addPageModal.classList.contains('active')) {
          hideAddPageModal();
        }
        
        const modal = document.getElementById('pre-designed-page-modal');
        if (modal && modal.classList.contains('active')) {
          closePreDesignedPageModal();
        }
      }
    });

    // Close modal on overlay click
    document.getElementById('add-page-modal').addEventListener('click', function(e) {
      if (e.target === this) {
        hideAddPageModal();
      }
    });
    
    // Calculate height on load
    calculateContainerHeight();
    
    // Recalculate on window resize
    window.addEventListener('resize', calculateContainerHeight);
    
    // Dark Mode Toggle
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    const editorWrapper = document.getElementById('visual-page-editor-wrapper');
    
    // Check for saved theme preference
    const savedTheme = localStorage.getItem('editor-theme') || 'light';
    if (savedTheme === 'dark') {
      editorWrapper.setAttribute('data-theme', 'dark');
      darkModeToggle.innerHTML = '<i class="${font:far()} fa-sun"></i>';
    }
    
    darkModeToggle.addEventListener('click', function() {
      const currentTheme = editorWrapper.getAttribute('data-theme');
      if (currentTheme === 'dark') {
        editorWrapper.removeAttribute('data-theme');
        darkModeToggle.innerHTML = '<i class="${font:far()} fa-moon"></i>';
        localStorage.setItem('editor-theme', 'light');
      } else {
        editorWrapper.setAttribute('data-theme', 'dark');
        darkModeToggle.innerHTML = '<i class="${font:far()} fa-sun"></i>';
        localStorage.setItem('editor-theme', 'dark');
      }
    });
    
    // Properties Panel Resizing
    const propertiesPanelElement = document.getElementById('properties-panel');
    const resizeHandle = document.getElementById('properties-panel-resize-handle');
    let isResizing = false;
    let startX = 0;
    let startWidth = 0;
    
    resizeHandle.addEventListener('mousedown', function(e) {
      isResizing = true;
      startX = e.clientX;
      startWidth = parseInt(window.getComputedStyle(propertiesPanelElement).width, 10);
      resizeHandle.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
      e.preventDefault();
    });
    
    document.addEventListener('mousemove', function(e) {
      if (!isResizing) return;
      
      const diff = startX - e.clientX; // Reverse because we're resizing from left
      const newWidth = startWidth + diff;
      const minWidth = 200;
      const maxWidth = 600;
      
      if (newWidth >= minWidth && newWidth <= maxWidth) {
        propertiesPanelElement.style.width = newWidth + 'px';
      }
    });
    
    document.addEventListener('mouseup', function() {
      if (isResizing) {
        isResizing = false;
        resizeHandle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        // Save width preference
        localStorage.setItem('properties-panel-width', propertiesPanelElement.style.width);
      }
    });
    
    // Restore saved width
    const savedWidth = localStorage.getItem('properties-panel-width');
    if (savedWidth) {
      propertiesPanelElement.style.width = savedWidth;
    }
  });
</script>
