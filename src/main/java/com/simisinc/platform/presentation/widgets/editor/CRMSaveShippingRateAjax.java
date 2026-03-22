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

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.ecommerce.SaveShippingRateCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.ShippingMethod;
import com.simisinc.platform.domain.model.ecommerce.ShippingRate;
import com.simisinc.platform.infrastructure.persistence.ecommerce.ShippingMethodRepository;
import com.simisinc.platform.infrastructure.persistence.ecommerce.ShippingRateRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to save (create/update) a shipping rate from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMSaveShippingRateAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908886L;
  private static Log LOG = LogFactory.getLog(CRMSaveShippingRateAjax.class);

  // GET: return available shipping methods for the modal
  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMSaveShippingRateAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    List<ShippingMethod> methods = ShippingMethodRepository.findAll();

    StringBuilder sb = new StringBuilder();
    sb.append("{\"shippingMethods\":[");
    boolean first = true;
    if (methods != null) {
      for (ShippingMethod m : methods) {
        if (!first)
          sb.append(",");
        first = false;
        sb.append("{\"id\":").append(m.getId()).append(",");
        sb.append("\"title\":\"").append(JsonCommand.toJson(m.getTitle())).append("\",");
        sb.append("\"code\":\"").append(JsonCommand.toJson(m.getCode())).append("\"}");
      }
    }
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }

  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMSaveShippingRateAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long id = context.getParameterAsLong("id", -1L);
    String countryCode = StringUtils.trimToNull(context.getParameter("countryCode"));
    String region = StringUtils.trimToNull(context.getParameter("region"));
    String postalCode = StringUtils.trimToNull(context.getParameter("postalCode"));
    long shippingMethodId = context.getParameterAsLong("shippingMethodId", -1L);
    String shippingFeeStr = StringUtils.trimToNull(context.getParameter("shippingFee"));
    String handlingFeeStr = StringUtils.trimToNull(context.getParameter("handlingFee"));
    String minSubTotalStr = StringUtils.trimToNull(context.getParameter("minSubTotal"));
    String displayText = StringUtils.trimToNull(context.getParameter("displayText"));

    try {
      ShippingRate rateBean;
      if (id > -1) {
        rateBean = ShippingRateRepository.findById(id);
        if (rateBean == null) {
          return context.writeError("Shipping rate not found");
        }
      } else {
        rateBean = new ShippingRate();
      }

      rateBean.setCountryCode(countryCode);
      rateBean.setRegion(region);
      rateBean.setPostalCode(postalCode);
      rateBean.setShippingMethodId((int) shippingMethodId);
      rateBean.setDisplayText(displayText);

      if (StringUtils.isNotBlank(shippingFeeStr)) {
        try {
          rateBean.setShippingFee(new BigDecimal(shippingFeeStr.trim()));
        } catch (NumberFormatException ignore) {
          rateBean.setShippingFee(BigDecimal.ZERO);
        }
      } else {
        rateBean.setShippingFee(BigDecimal.ZERO);
      }

      if (StringUtils.isNotBlank(handlingFeeStr)) {
        try {
          rateBean.setHandlingFee(new BigDecimal(handlingFeeStr.trim()));
        } catch (NumberFormatException ignore) {
          rateBean.setHandlingFee(BigDecimal.ZERO);
        }
      } else {
        rateBean.setHandlingFee(BigDecimal.ZERO);
      }

      if (StringUtils.isNotBlank(minSubTotalStr)) {
        try {
          rateBean.setMinSubTotal(new BigDecimal(minSubTotalStr.trim()));
        } catch (NumberFormatException ignore) {
          // use null/default
        }
      }

      ShippingRate saved = SaveShippingRateCommand.saveShippingRate(rateBean);
      if (saved == null) {
        throw new DataException("Shipping rate could not be saved");
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Shipping rate saved\",");
      sb.append("\"rate\":{");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"countryCode\":\"").append(JsonCommand.toJson(saved.getCountryCode() != null ? saved.getCountryCode() : ""))
          .append("\",");
      sb.append("\"region\":\"").append(JsonCommand.toJson(saved.getRegion() != null ? saved.getRegion() : "")).append("\"");
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
