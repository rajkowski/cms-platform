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
package com.zeroio.platform.application.cms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import com.simisinc.platform.application.cms.LoadFileCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;

/**
 * DiagramHtmlCommand
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DiagramHtmlCommand {

  private static Log LOG = LogFactory.getLog(DiagramHtmlCommand.class);

  private static final Pattern DIAGRAM_TOKEN_PATTERN = Pattern.compile("\\$\\{diagram:([^;\\}\\s]+)(?:;([^\\}]*))?\\}");

  private static boolean canAccessDiagram(WidgetContext context, String webPath, FileItem fileItem) {
    if (fileItem == null) {
      return false;
    }
    // Keep legacy behavior for static/unit calls that do not provide a request context.
    if (context == null) {
      return true;
    }
    if (context.hasRole("admin") || context.hasRole("content-manager")) {
      return true;
    }
    return LoadFileCommand.loadLatestFileByIdForAuthorizedUser(webPath, fileItem.getId(), context.getUserId()) != null;
  }

  /**
   * For the given uniqueId, replace any dynamic values
   *  
   * @param context
   * @param uniqueId
   * @return
   */
  public static String replaceDiagramTokens(String html) {
    return replaceDiagramTokens(null, html);
  }

  public static String replaceDiagramTokens(WidgetContext context, String html) {
    if (html == null || !html.contains("${diagram:")) {
      return html;
    }
    Matcher matcher = DIAGRAM_TOKEN_PATTERN.matcher(html);
    StringBuffer renderedHtml = new StringBuffer();
    while (matcher.find()) {
      String webPath = matcher.group(1).trim();
      String label = matcher.group(2) != null ? matcher.group(2).trim() : null;
      LOG.debug("Found diagram token: webPath=" + webPath + ", label=" + label);
      FileItem fileItem = FileItemRepository.findByWebPath(webPath);
      String replacement = buildDiagramHtml(context, webPath, label, fileItem);
      matcher.appendReplacement(renderedHtml, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(renderedHtml);
    return renderedHtml.toString();
  }

  private static String buildDiagramHtml(WidgetContext context, String webPath, String label, FileItem fileItem) {
    String displayName;
    String viewUrl = null;
    String downloadUrl = null;
    String previewImageUrl = null;
    boolean canAccessDiagram = canAccessDiagram(context, webPath, fileItem);
    if (fileItem != null) {
      displayName = StringUtils.defaultIfBlank(label, StringUtils.defaultIfBlank(fileItem.getTitle(), webPath));
      if (canAccessDiagram) {
        viewUrl = "/assets/drawio/" + fileItem.getUrl();
        downloadUrl = "/assets/file/" + fileItem.getUrl();
      } else {
        Image previewImage = findDiagramPreviewImage(webPath);
        if (previewImage != null) {
          previewImageUrl = "/assets/img/" + previewImage.getUrl();
        }
      }
    } else {
      displayName = StringUtils.defaultIfBlank(label, webPath);
    }
    StringBuilder html = new StringBuilder();
    html.append("<div class=\"drawio-diagram\" style=\"margin: 10px 0;\">");
    if (viewUrl != null) {
      String iframeId = "drawio-frame-" + Math.abs(webPath.hashCode());
      String embeddedViewUrl = viewUrl + "?embedded=true&frameId=" + iframeId;
      html.append(
          "<div class=\"drawio-diagram-viewer\" style=\"margin: 0 0 10px 0; border: 1px solid #ddd; background: #fff;\">")
          .append("<iframe id=\"")
          .append(iframeId)
          .append("\" src=\"")
          .append(StringEscapeUtils.escapeHtml4(embeddedViewUrl))
          .append("\" title=\"")
          .append(StringEscapeUtils.escapeHtml4(displayName))
          .append(
              "\" loading=\"lazy\" scrolling=\"no\" style=\"width: 100%; min-height: 420px; border: 0; overflow: hidden;\"></iframe>")
          .append("<script>(function(){var frame=document.getElementById('")
          .append(iframeId)
          .append(
              "');if(!frame){return;}var resizeFrame=function(){try{var doc=frame.contentWindow.document;" +
                  "var body=doc.body;var htmlEl=doc.documentElement;var nextHeight=Math.max(body?body.scrollHeight:0,htmlEl?htmlEl.scrollHeight:0,420);frame.style.height=nextHeight+'px';}catch(e){}};"
                  +
                  "frame.addEventListener('load',function(){resizeFrame();setTimeout(resizeFrame,250);setTimeout(resizeFrame,1000);setTimeout(resizeFrame,2000);});"
                  +
                  "window.addEventListener('message',function(event){if(event&&event.data&&event.data.type==='drawio-embed-height'&&event.data.frameId==='")
          .append(iframeId)
          .append(
              "'){var h=parseInt(event.data.height,10);if(!isNaN(h)&&h>0){frame.style.height=Math.max(420,h)+'px';}}});})();</script>")
          .append("</div>");
    } else if (previewImageUrl != null) {
      html.append(
          "<div class=\"drawio-diagram-preview\" style=\"margin: 0 0 10px 0; border: 1px solid #ddd; background: #fff; text-align: center;\">")
          .append("<img src=\"")
          .append(StringEscapeUtils.escapeHtml4(previewImageUrl))
          .append("\" alt=\"")
          .append(StringEscapeUtils.escapeHtml4(displayName))
          .append("\" style=\"max-width: 100%; height: auto; display: inline-block;\" />")
          .append("</div>");
    }
    html.append(
        "<div class=\"drawio-diagram-actions\" style=\"display: flex; gap: 8px; flex-wrap: wrap; align-items: center; padding: 8px 10px; border: 1px solid #ddd; background: #f8f9fa;\">");
    html.append("<span style=\"font-weight: 600;\">")
        .append(StringEscapeUtils.escapeHtml4(displayName))
        .append("</span>");
    if (viewUrl != null) {
      html.append(" <a href=\"")
          .append(StringEscapeUtils.escapeHtml4(viewUrl))
          .append("\" target=\"_blank\" rel=\"noopener noreferrer\" class=\"button tiny primary no-gap\">View</a>");
    }
    if (downloadUrl != null) {
      html.append(" <a href=\"")
          .append(StringEscapeUtils.escapeHtml4(downloadUrl))
          .append("\" download class=\"button tiny primary no-gap\">Download</a>");
    } else if (fileItem != null && !canAccessDiagram) {
      html.append(
          " <span class=\"button tiny primary no-gap disabled\" aria-disabled=\"true\" title=\"You do not have permission to view this diagram\">View</span>");
      html.append(
          " <span class=\"button tiny secondary no-gap disabled\" aria-disabled=\"true\" title=\"You do not have permission to download this diagram\">Download</span>");
    }
    html.append("</div></div>");
    return html.toString();
  }

  private static Image findDiagramPreviewImage(String webPath) {
    if (StringUtils.isBlank(webPath)) {
      return null;
    }
    try {
      Image image = ImageRepository.findByWebPath(webPath);
      if (image != null) {
        return image;
      }
      if (webPath.startsWith(IntegrationAttachmentCommand.INTEGRATION_PREFIX)) {
        String numericPart = Strings.CS.removeStart(webPath, IntegrationAttachmentCommand.INTEGRATION_PREFIX);
        if (StringUtils.isNumeric(numericPart)) {
          long sequence = Long.parseLong(numericPart);
          return ImageRepository.findByWebPath(IntegrationAttachmentCommand.INTEGRATION_PREFIX + (sequence + 1));
        }
      }
    } catch (Exception e) {
      LOG.debug("Diagram preview image lookup failed for webPath: " + webPath, e);
    }
    return null;
  }
}
