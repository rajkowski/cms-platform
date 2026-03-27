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
import com.simisinc.platform.application.ecommerce.SaveSalesTaxNexusAddressCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.SalesTaxNexusAddress;
import com.simisinc.platform.infrastructure.persistence.ecommerce.SalesTaxNexusAddressRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to save (create/update) a sales tax nexus address from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMSaveSalesTaxNexusAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908885L;
  private static Log LOG = LogFactory.getLog(CRMSaveSalesTaxNexusAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMSaveSalesTaxNexusAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long id = context.getParameterAsLong("id", -1L);
    String street = StringUtils.trimToNull(context.getParameter("street"));
    String addressLine2 = StringUtils.trimToNull(context.getParameter("addressLine2"));
    String city = StringUtils.trimToNull(context.getParameter("city"));
    String state = StringUtils.trimToNull(context.getParameter("state"));
    String country = StringUtils.trimToNull(context.getParameter("country"));
    String postalCode = StringUtils.trimToNull(context.getParameter("postalCode"));

    try {
      SalesTaxNexusAddress addressBean;
      if (id > -1) {
        addressBean = SalesTaxNexusAddressRepository.findById(id);
        if (addressBean == null) {
          context.setJson("{\"success\":false,\"message\":\"Sales tax nexus address not found\"}");
          context.setSuccess(false);
          return context;
        }
      } else {
        addressBean = new SalesTaxNexusAddress();
      }

      addressBean.setStreet(street);
      addressBean.setAddressLine2(addressLine2);
      addressBean.setCity(city);
      addressBean.setState(state);
      addressBean.setCountry(country);
      addressBean.setPostalCode(postalCode);
      addressBean.setCreatedBy(context.getUserId());
      addressBean.setModifiedBy(context.getUserId());

      SalesTaxNexusAddress saved = SaveSalesTaxNexusAddressCommand.saveAddress(addressBean);
      if (saved == null) {
        throw new DataException("Sales tax nexus address could not be saved");
      }

      String label = saved.getState() != null ? saved.getState()
          : (saved.getCity() != null ? saved.getCity() : saved.getCountry());

      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Sales tax nexus address saved\",");
      sb.append("\"nexus\":{");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"label\":\"").append(JsonCommand.toJson(label != null ? label : "")).append("\",");
      sb.append("\"state\":\"").append(JsonCommand.toJson(saved.getState() != null ? saved.getState() : "")).append("\",");
      sb.append("\"country\":\"").append(JsonCommand.toJson(saved.getCountry() != null ? saved.getCountry() : "")).append("\"");
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
