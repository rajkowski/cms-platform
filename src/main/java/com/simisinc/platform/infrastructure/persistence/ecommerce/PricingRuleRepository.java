/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.infrastructure.persistence.ecommerce;

import static java.util.stream.Collectors.toList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.ecommerce.PricingRule;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves pricing rule objects
 *
 * @author matt rajkowski
 * @created 11/21/19 8:53 PM
 */
public class PricingRuleRepository {

  private static Log LOG = LogFactory.getLog(PricingRuleRepository.class);

  private static String TABLE_NAME = "pricing_rules";
  private static String[] PRIMARY_KEY = new String[] { "rule_id" };

  private static DataResult<PricingRule> query(PricingRuleSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (StringUtils.isNotBlank(specification.getCountryCode())) {
        select.AND("valid_country_code IS NULL OR valid_country_code = ?", specification.getCountryCode());
      }
      if (StringUtils.isNotBlank(specification.getPromoCode())) {
        select.AND("upper(promo_code) = ?", specification.getPromoCode().toUpperCase());
      }
      if (specification.getEnabled() != DataConstants.UNDEFINED) {
        select.AND("enabled = ?", specification.getEnabled() == DataConstants.TRUE);
      }
      if (specification.getIsValidToday() == DataConstants.TRUE) {
        select.AND("(from_date IS NULL OR from_date <= CURRENT_TIMESTAMP)");
        select.AND("(to_date IS NULL OR to_date > CURRENT_TIMESTAMP)");
      }
      if (specification.getHasPromoCode() != DataConstants.UNDEFINED) {
        select.AND("promo_code IS NULL OR promo_code = ''");
      }
      if (StringUtils.isNotBlank(specification.getIncludesSku())) {
        select.AND("LOWER(valid_skus) LIKE LOWER(?) ESCAPE '!'", "%" + specification.getIncludesSku().toLowerCase() + "%");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(PricingRuleRepository::buildRecord);
  }

  public static List<PricingRule> findAll(PricingRuleSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("to_date desc, created");
    return query(specification, constraints).getRecords();
  }

  public static List<PricingRule> findAllRulesByValidSku(String sku) {
    PricingRuleSpecification specification = new PricingRuleSpecification();
    specification.setEnabled(true);
    specification.setHasPromoCode(false);
    specification.setIncludesSku(sku);
    List<PricingRule> recordList = query(specification, null).getRecords();
    List<PricingRule> pricingRuleList = new ArrayList<>();
    for (PricingRule record : recordList) {
      List<String> validSkuList = Stream.of(record.getValidSkus().toUpperCase().split(Pattern.quote(",")))
          .map(String::trim)
          .collect(toList());
      if (validSkuList.contains(sku.toUpperCase())) {
        pricingRuleList.add(record);
      }
    }
    return pricingRuleList;
  }

  public static PricingRule findById(long ruleId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("rule_id = ?", ruleId)
        .returnRecord(PricingRuleRepository::buildRecord);
  }

  public static PricingRule save(PricingRule record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static PricingRule add(PricingRule record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      record.setId(DB.INSERT().INTO(TABLE_NAME)
          .FIELD("name", record.getName())
          .FIELD("description", record.getDescription())
          .FIELD("error_message", record.getErrorMessage())
          .FIELD("from_date", record.getFromDate())
          .FIELD("to_date", record.getToDate())
          .FIELD("promo_code", record.getPromoCode())
          .FIELD("uses_per_code", record.getUsesPerCode())
          .FIELD("uses_per_customer", record.getUsesPerCustomer())
          .FIELD("times_used", record.getTimesUsed())
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .FIELD("enabled", record.getEnabled())
          .FIELD("minimum_subtotal", record.getMinimumSubtotal())
          .FIELD("minimum_order_qty", record.getMinimumOrderQuantity())
          .FIELD("maximum_order_qty", record.getMaximumOrderQuantity())
          .FIELD("item_limit", record.getItemLimit())
          .FIELD("valid_skus", record.getValidSkus())
          .FIELD("invalid_skus", record.getInvalidSkus())
          .FIELD("subtotal_percent", record.getSubtotalPercent())
          .FIELD("subtract_amount", record.getSubtractAmount())
          .FIELD("buy_x_items", record.getBuyXItems())
          .FIELD("get_y_free", record.getGetYItemsFree())
          .FIELD("free_shipping", record.getFreeShipping())
          .FIELD("free_product_sku", record.getFreeProductSku())
          .FIELD("free_shipping_code", record.getFreeShippingCode())
          .FIELD("valid_country_code", record.getCountryCode())
          .execute(connection));
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static PricingRule update(PricingRule record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("name", record.getName())
        .SET("description", record.getDescription())
        .SET("error_message", record.getErrorMessage())
        .SET("from_date", record.getFromDate())
        .SET("to_date", record.getToDate())
        .SET("promo_code", record.getPromoCode())
        .SET("uses_per_code", record.getUsesPerCode())
        .SET("uses_per_customer", record.getUsesPerCustomer())
        .SET("enabled", record.getEnabled())
        .SET("minimum_subtotal", record.getMinimumSubtotal())
        .SET("minimum_order_qty", record.getMinimumOrderQuantity())
        .SET("maximum_order_qty", record.getMaximumOrderQuantity())
        .SET("item_limit", record.getItemLimit())
        .SET("valid_skus", record.getValidSkus())
        .SET("invalid_skus", record.getInvalidSkus())
        .SET("subtotal_percent", record.getSubtotalPercent())
        .SET("subtract_amount", record.getSubtractAmount())
        .SET("buy_x_items", record.getBuyXItems())
        .SET("get_y_free", record.getGetYItemsFree())
        .SET("free_shipping", record.getFreeShipping())
        .SET("free_product_sku", record.getFreeProductSku())
        .SET("free_shipping_code", record.getFreeShippingCode())
        .SET("valid_country_code", record.getCountryCode())
        .SET("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("rule_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static PricingRule buildRecord(ResultSet rs) {
    try {
      PricingRule record = new PricingRule();
      record.setId(rs.getLong("rule_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setErrorMessage(rs.getString("error_message"));
      record.setFromDate(rs.getTimestamp("from_date"));
      record.setToDate(rs.getTimestamp("to_date"));
      record.setPromoCode(rs.getString("promo_code"));
      record.setUsesPerCode(DB.getInt(rs, "uses_per_code", 0));
      record.setTimesUsed(rs.getInt("times_used"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setMinimumSubtotal(rs.getBigDecimal("minimum_subtotal"));
      record.setMinimumOrderQuantity(rs.getInt("minimum_order_qty"));
      record.setMaximumOrderQuantity(rs.getInt("maximum_order_qty"));
      record.setValidSkus(rs.getString("valid_skus"));
      record.setInvalidSkus(rs.getString("invalid_skus"));
      record.setSubtotalPercent(rs.getInt("subtotal_percent"));
      record.setSubtractAmount(rs.getBigDecimal("subtract_amount"));
      record.setFreeShipping(rs.getBoolean("free_shipping"));
      record.setFreeProductSku(rs.getString("free_product_sku"));
      record.setFreeShippingCode(rs.getString("free_shipping_code"));
      record.setCountryCode(rs.getString("valid_country_code"));
      record.setUsesPerCustomer(DB.getInt(rs, "uses_per_customer", 0));
      record.setItemLimit(DB.getInt(rs, "item_limit", 0));
      record.setBuyXItems(DB.getInt(rs, "buy_x_items", 0));
      record.setGetYItemsFree(DB.getInt(rs, "get_y_free", 0));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
