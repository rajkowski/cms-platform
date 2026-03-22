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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.ecommerce.SavePricingRuleCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.PricingRule;
import com.simisinc.platform.infrastructure.persistence.ecommerce.PricingRuleRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to save (create/update) a pricing rule from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMSavePricingRuleAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908884L;
  private static Log LOG = LogFactory.getLog(CRMSavePricingRuleAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMSavePricingRuleAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long id = context.getParameterAsLong("id", -1L);
    String name = StringUtils.trimToNull(context.getParameter("name"));
    String description = StringUtils.trimToNull(context.getParameter("description"));
    String promoCode = StringUtils.trimToNull(context.getParameter("promoCode"));
    boolean enabled = !"false".equalsIgnoreCase(context.getParameter("enabled"));
    String subtotalPercentStr = StringUtils.trimToNull(context.getParameter("subtotalPercent"));
    String subtractAmountStr = StringUtils.trimToNull(context.getParameter("subtractAmount"));
    boolean freeShipping = "true".equalsIgnoreCase(context.getParameter("freeShipping"));

    if (StringUtils.isBlank(name)) {
      return context.writeError("A name is required");
    }

    try {
      PricingRule ruleBean;
      if (id > -1) {
        ruleBean = PricingRuleRepository.findById(id);
        if (ruleBean == null) {
          return context.writeError("Pricing rule not found");
        }
      } else {
        ruleBean = new PricingRule();
      }

      ruleBean.setName(name);
      ruleBean.setDescription(description);
      ruleBean.setPromoCode(promoCode);
      ruleBean.setEnabled(enabled);
      ruleBean.setFreeShipping(freeShipping);
      ruleBean.setCreatedBy(context.getUserId());
      ruleBean.setModifiedBy(context.getUserId());

      if (StringUtils.isNotBlank(subtotalPercentStr)) {
        try {
          ruleBean.setSubtotalPercent(Integer.parseInt(subtotalPercentStr.trim()));
        } catch (NumberFormatException ignore) {
          // ignore invalid value
        }
      }
      if (StringUtils.isNotBlank(subtractAmountStr)) {
        try {
          ruleBean.setSubtractAmount(new BigDecimal(subtractAmountStr.trim()));
        } catch (NumberFormatException ignore) {
          // ignore invalid value
        }
      }

      PricingRule saved = SavePricingRuleCommand.savePricingRule(ruleBean);
      if (saved == null) {
        throw new DataException("Pricing rule could not be saved");
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Pricing rule saved\",");
      sb.append("\"pricingRule\":{");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(saved.getName())).append("\"");
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
