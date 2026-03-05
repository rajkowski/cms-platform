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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.ecommerce.SaveProductCategoryCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.ProductCategory;
import com.simisinc.platform.infrastructure.persistence.ecommerce.ProductCategoryRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to save (create/update) a product category from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMSaveProductCategoryAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908882L;
  private static Log LOG = LogFactory.getLog(CRMSaveProductCategoryAjax.class);

  public WidgetContext post(WidgetContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    long id = context.getParameterAsLong("id", -1L);
    String name = StringUtils.trimToNull(context.getParameter("name"));
    String uniqueId = StringUtils.trimToNull(context.getParameter("uniqueId"));
    String description = StringUtils.trimToNull(context.getParameter("description"));
    boolean enabled = !"false".equalsIgnoreCase(context.getParameter("enabled"));

    if (StringUtils.isBlank(name)) {
      context.setJson("{\"success\":false,\"message\":\"A name is required\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      ProductCategory categoryBean;
      if (id > -1) {
        categoryBean = ProductCategoryRepository.findById(id);
        if (categoryBean == null) {
          context.setJson("{\"success\":false,\"message\":\"Category not found\"}");
          context.setSuccess(false);
          return context;
        }
      } else {
        categoryBean = new ProductCategory();
      }

      categoryBean.setName(name);
      if (StringUtils.isNotBlank(uniqueId)) {
        categoryBean.setUniqueId(uniqueId);
      }
      categoryBean.setDescription(description);
      categoryBean.setEnabled(enabled);
      categoryBean.setCreatedBy(context.getUserId());
      categoryBean.setModifiedBy(context.getUserId());

      ProductCategory saved = SaveProductCategoryCommand.save(categoryBean);
      if (saved == null) {
        throw new DataException("Product category could not be saved");
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Product category saved\",");
      sb.append("\"category\":{");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(saved.getName())).append("\",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(saved.getUniqueId() != null ? saved.getUniqueId() : "")).append("\"");
      sb.append("}}");

      context.setJson(sb.toString());
      return context;

    } catch (DataException de) {
      LOG.error("DataException", de);
      context.setJson("{\"success\":false,\"message\":\"" + JsonCommand.toJson(de.getMessage()) + "\"}");
      context.setSuccess(false);
      return context;
    } catch (Exception e) {
      LOG.error("Exception", e);
      context.setJson("{\"success\":false,\"message\":\"An error occurred\"}");
      context.setSuccess(false);
      return context;
    }
  }
}
