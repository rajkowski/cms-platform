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
import com.simisinc.platform.domain.model.ecommerce.Customer;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.ecommerce.CustomerRepository;
import com.simisinc.platform.infrastructure.persistence.ecommerce.CustomerSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to list customers with search support
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMCustomersJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908902L;
  private static Log LOG = LogFactory.getLog(CRMCustomersJsonService.class);

  public JsonServiceContext get(JsonServiceContext context) {

    if (!context.hasRole("admin") && !context.hasRole("ecommerce-manager")) {
      return context.writeError("Permission Denied");
    }

    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("limit", 25);
    String searchTerm = context.getParameter("search");
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);

    CustomerSpecification specification = new CustomerSpecification();
    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setName(searchTerm);
    }

    List<Customer> customers = CustomerRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"customers\":[");
    boolean first = true;
    for (Customer customer : customers) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{");
      sb.append("\"id\":").append(customer.getId()).append(",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(customer.getUniqueId()))).append("\",");
      sb.append("\"email\":\"").append(JsonCommand.toJson(StringUtils.defaultString(customer.getEmail()))).append("\",");
      sb.append("\"firstName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(customer.getFirstName()))).append("\",");
      sb.append("\"lastName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(customer.getLastName()))).append("\",");
      sb.append("\"organization\":\"").append(JsonCommand.toJson(StringUtils.defaultString(customer.getOrganization()))).append("\",");
      sb.append("\"orderCount\":").append(customer.getOrderCount()).append(",");
      sb.append("\"totalSpend\":").append(customer.getTotalSpend() != null ? customer.getTotalSpend().toPlainString() : "0")
          .append(",");
      sb.append("\"created\":\"").append(customer.getCreated() != null ? customer.getCreated().toString() : "").append("\"");
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
