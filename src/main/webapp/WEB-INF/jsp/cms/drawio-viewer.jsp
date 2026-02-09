<%--
  ~ Copyright 2026 Matt Rajkowski
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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="highlight" class="java.lang.String" scope="request"/>
<jsp:useBean id="toolbar" class="java.lang.String" scope="request"/>
<jsp:useBean id="toolbarPosition" class="java.lang.String" scope="request"/>
<jsp:useBean id="toolbarNohide" class="java.lang.String" scope="request"/>
<jsp:useBean id="lightbox" class="java.lang.String" scope="request"/>
<jsp:useBean id="editable" class="java.lang.String" scope="request"/>
<jsp:useBean id="zoom" class="java.lang.String" scope="request"/>
<jsp:useBean id="nav" class="java.lang.String" scope="request"/>
<jsp:useBean id="resize" class="java.lang.String" scope="request"/>
<jsp:useBean id="edit" class="java.lang.String" scope="request"/>
<jsp:useBean id="filePath" class="java.lang.String" scope="request"/>

<script src="${ctx}/javascript/drawio-29.3.6/viewer.min.js"></script>

<div style="width: 100%; overflow-x: auto;">
    <div id="drawio-diagram"></div>
</div>

<script>
    (function () {
        // Escape HTML
        const chatMap = {
            "&": "&amp;",
            "'": "&#x27;",
            "`": "&#x60;",
            '"': "&quot;",
            "<": "&lt;",
            ">": "&gt;",
        };

        function replaceMatchedCharacters(match) {
            return chatMap[match];
        }

        function escapeHTML(string) {
            if (typeof string !== "string") return string;
            return string.replace(/[&'`"<>]/g, replaceMatchedCharacters);
        }

        // Draw.io converter
        function createMxGraphData(xml) {
            return {
                editable: false,
                highlight: "<c:out value='${highlight}' />",
                nav: <c:out value='${nav}' />,
                toolbar: <c:choose><c:when test="${empty toolbar}">null</c:when><c:otherwise>"<c:out value='${toolbar}'/>"</c:otherwise></c:choose>,
                "toolbar-position": "<c:out value='${toolbarPosition}' />",
                "toolbar-nohide": <c:out value='${toolbarNohide}' />,
                lightbox: <c:choose><c:when test="${lightbox eq 'true'}">"open"</c:when><c:otherwise>false</c:otherwise></c:choose>,
                edit: <c:choose><c:when test="${empty edit or 'false' eq edit}">null</c:when><c:otherwise>"true"</c:otherwise></c:choose>,
                resize: <c:out value='${resize}' />,
                zoom: <c:out value='${zoom}' />,
                target: '_blank',
                xml,
            };
        }

        async function drawioConverterAsync(xml) {
            return new Promise((resolve) => {
                const mxGraphData = createMxGraphData(xml);
                const json = JSON.stringify(mxGraphData);
                const mxGraphHTML = createMxGraphHTML(json);
                resolve(mxGraphHTML);
            });
        }

        function createMxGraphHTML(json) {
            return '<div class="mxgraph" style="max-width:100%;border:1px solid transparent;" data-mxgraph="' + escapeHTML(json) + '"></div>';
        }

        // Load Draw.io file
        function loadDrawioFile(url) {
            fetch(url)
                .then((response) => {
                    if (!response.ok) {
                        throw new Error("Failed to load diagram: " + response.statusText);
                    }
                    return response.text();
                })
                .then((data) => {
                    return drawioConverterAsync(data);
                })
                .then((content) => {
                    const graphContainer = document.getElementById('drawio-diagram');
                    graphContainer.innerHTML = content;
                    if (window.GraphViewer) {
                        window.GraphViewer.processElements();
                    }
                })
                .catch((err) => {
                    console.error("Error loading draw.io file:", err);
                    const graphContainer = document.getElementById('drawio-diagram');
                    graphContainer.innerHTML = '<div style="color: red; padding: 10px;">Error loading diagram: ' + err.message + '</div>';
                });
        }

        // Render the file
        loadDrawioFile("/assets/file/${filePath}");
    })();
</script>
