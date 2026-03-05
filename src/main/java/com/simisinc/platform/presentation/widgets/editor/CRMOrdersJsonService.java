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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.Order;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.ecommerce.OrderRepository;
import com.simisinc.platform.infrastructure.persistence.ecommerce.OrderSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to list orders, optionally filtered by customer
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMOrdersJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908903L;
  private static Log LOG = LogFactory.getLog(CRMOrdersJsonService.class);

  public WidgetContext execute(WidgetContext context) {

    if (!context.hasRole("admin") && !context.hasRole("ecommerce-manager")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("limit", 25);
    long customerId = context.getParameterAsLong("customerId");
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);

    OrderSpecification specification = new OrderSpecification();
    if (customerId != -1) {
      specification.setCustomerId(customerId);
    }

    List<Order> orders = OrderRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"orders\":[");
    boolean first = true;
    for (Order order : orders) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{");
      sb.append("\"id\":").append(order.getId()).append(",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(order.getUniqueId()))).append("\",");
      sb.append("\"email\":\"").append(JsonCommand.toJson(StringUtils.defaultString(order.getEmail()))).append("\",");
      sb.append("\"firstName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(order.getFirstName()))).append("\",");
      sb.append("\"lastName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(order.getLastName()))).append("\",");
      sb.append("\"customerId\":").append(order.getCustomerId()).append(",");
      sb.append("\"totalAmount\":").append(order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0").append(",");
      sb.append("\"statusId\":").append(order.getStatusId()).append(",");
      sb.append("\"paid\":").append(order.getPaid()).append(",");
      sb.append("\"processed\":").append(order.getProcessed()).append(",");
      sb.append("\"shipped\":").append(order.getShipped()).append(",");
      sb.append("\"canceled\":").append(order.getCanceled()).append(",");
      sb.append("\"created\":\"").append(order.getCreated() != null ? order.getCreated().toString() : "").append("\"");
      sb.append("}");
    }
    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(itemsPerPage).append(",");
    sb.append("\"total\":").append(constraints.getTotalRecordCount());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
