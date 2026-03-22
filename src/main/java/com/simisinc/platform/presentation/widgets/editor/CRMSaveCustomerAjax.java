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
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.ecommerce.SaveCustomerCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.Customer;
import com.simisinc.platform.infrastructure.persistence.ecommerce.CustomerRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to save (create/update) a customer from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMSaveCustomerAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908881L;
  private static Log LOG = LogFactory.getLog(CRMSaveCustomerAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMSaveCustomerAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long id = context.getParameterAsLong("id", -1L);
    String firstName = StringUtils.trimToNull(context.getParameter("firstName"));
    String lastName = StringUtils.trimToNull(context.getParameter("lastName"));
    String email = StringUtils.trimToNull(context.getParameter("email"));
    String organization = StringUtils.trimToNull(context.getParameter("organization"));

    if (StringUtils.isBlank(firstName)) {
      return context.writeError("A first name is required");
    }
    if (StringUtils.isBlank(lastName)) {
      return context.writeError("A last name is required");
    }
    if (StringUtils.isBlank(email)) {
      return context.writeError("An email address is required");
    }

    try {
      Customer customer;
      if (id > -1) {
        customer = CustomerRepository.findById(id);
        if (customer == null) {
          context.setJson("{\"success\":false,\"message\":\"Customer not found\"}");
          context.setSuccess(false);
          return context;
        }
      } else {
        customer = new Customer();
      }

      customer.setFirstName(firstName);
      customer.setLastName(lastName);
      customer.setEmail(email);
      customer.setOrganization(organization);

      Customer saved = CustomerRepository.save(customer);
      if (saved == null) {
        throw new DataException("Customer could not be saved");
      }

      // Generate uniqueId if new
      if (id <= -1 && StringUtils.isBlank(saved.getUniqueId())) {
        try {
          saved.setUniqueId(SaveCustomerCommand.generateUniqueId(saved));
          CustomerRepository.save(saved);
        } catch (Exception ignore) {
          LOG.warn("Could not generate customer uniqueId: " + ignore.getMessage());
        }
      }

      String name = (firstName + " " + lastName).trim();

      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Customer saved\",");
      sb.append("\"customer\":{");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"firstName\":\"").append(JsonCommand.toJson(saved.getFirstName())).append("\",");
      sb.append("\"lastName\":\"").append(JsonCommand.toJson(saved.getLastName())).append("\",");
      sb.append("\"email\":\"").append(JsonCommand.toJson(saved.getEmail() != null ? saved.getEmail() : "")).append("\",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(name)).append("\"");
      sb.append("}}");

      context.setJson(sb.toString());
      return context;

    } catch (DataException de) {
      LOG.error("DataException", de);
      return context.writeError("" + de.getMessage());
    } catch (Exception e) {
      LOG.error("Exception", e);
      return context.writeError("An error occurred");
    }
  }
}
