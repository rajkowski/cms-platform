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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.ecommerce.ShippingRate;

/**
 * Persists and retrieves shipping rate objects
 *
 * @author matt rajkowski
 * @created 5/8/19 9:21 AM
 */
public class ShippingRateRepository {

  private static Log LOG = LogFactory.getLog(ShippingRateRepository.class);

  private static String TABLE_NAME = "shipping_rates";
  private static String ADDITIONAL_SELECT = "lookup_shipping_method.title";
  private static String JOIN = "LEFT JOIN lookup_shipping_method ON (shipping_rates.shipping_method = lookup_shipping_method.method_id)";
  private static String[] PRIMARY_KEY = new String[] { "rate_id" };

  private static DataResult<ShippingRate> query(ShippingRateSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("shipping_rates.*", ADDITIONAL_SELECT).FROM(TABLE_NAME).WHERE();
    select.LEFT_JOIN("lookup_shipping_method")
        .ON("shipping_rates.shipping_method = lookup_shipping_method.method_id");

    if (specification != null) {
      if (StringUtils.isNotBlank(specification.getCountryCode())) {
        select.AND("country_code = ?", specification.getCountryCode());
      }
      if (StringUtils.isNotBlank(specification.getRegion()) && StringUtils.isNotBlank(specification.getPostalCode())) {
        String region = specification.getRegion();
        String postalCode = specification.getPostalCode();
        if ("*".equals(region) && "*".equals(postalCode)) {
          select.AND("postal_code = '*'");
          select.AND("region = '*'");
        } else {
          if ("US".equals(specification.getCountryCode()) && postalCode.length() > 5) {
            postalCode = postalCode.substring(0, 5);
          }
          if (specification.getSpecificRegionOnly()) {
            select.AND("(postal_code = ? OR (postal_code = '*' AND region = ?))", postalCode, region);
          } else {
            select.AND("(postal_code = ? OR (postal_code = '*' AND region = ?) OR (postal_code = '*' AND region = '*'))",
                postalCode, region);
          }
        }
      }
      if (specification.getOrderSubtotal() != null) {
        select.AND("min_subtotal <= ?", specification.getOrderSubtotal());
      }
      if (specification.getPackageTotalWeightOz() >= 0) {
        select.AND("min_weight_oz <= ?", specification.getPackageTotalWeightOz());
      }
      if (specification.getEnabledOnly()) {
        select.AND("lookup_shipping_method.enabled = ?", true);
      }
      constraints.setDefaultColumnToSortBy("postal_code, region, shipping_method, shipping_fee");
    }

    return select.WITH(constraints).returnDataResult(ShippingRateRepository::buildRecord);
  }

  public static List<ShippingRate> findAll(ShippingRateSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("country_code, region, postal_code, shipping_method, shipping_fee");
    return query(specification, constraints).getRecords();
  }

  public static ShippingRate findById(long shippingRateId) {
    return DB.SELECT("shipping_rates.*", ADDITIONAL_SELECT)
        .FROM(TABLE_NAME)
        .LEFT_JOIN("lookup_shipping_method")
        .ON("shipping_rates.shipping_method = lookup_shipping_method.method_id")
        .WHERE("rate_id = ?", shippingRateId)
        .returnRecord(ShippingRateRepository::buildRecord);
  }

  public static ShippingRate save(ShippingRate record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static ShippingRate add(ShippingRate record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      long generatedId = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("country_code", record.getCountryCode())
          .FIELD("region", record.getRegion())
          .FIELD("postal_code", record.getPostalCode())
          .FIELD("min_subtotal", record.getMinSubTotal())
          .FIELD("min_weight_oz", record.getMinWeightOz())
          .FIELD("shipping_fee", record.getShippingFee())
          .FIELD("handling_fee", record.getHandlingFee())
          .FIELD("shipping_code", record.getShippingCode())
          .FIELD("shipping_method", record.getShippingMethodId())
          .FIELD("display_text", record.getDisplayText())
          .FIELD("exclude_skus", record.getExcludeSkus())
          .execute(connection);
      record.setId(generatedId);
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static ShippingRate update(ShippingRate record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("country_code", record.getCountryCode())
        .SET("region", record.getRegion())
        .SET("postal_code", record.getPostalCode())
        .SET("min_subtotal", record.getMinSubTotal())
        .SET("min_weight_oz", record.getMinWeightOz())
        .SET("shipping_fee", record.getShippingFee())
        .SET("handling_fee", record.getHandlingFee())
        .SET("shipping_code", record.getShippingCode())
        .SET("shipping_method", record.getShippingMethodId())
        .SET("display_text", record.getDisplayText())
        .SET("exclude_skus", record.getExcludeSkus())
        .WHERE("rate_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(ShippingRate record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("rate_id = ?", record.getId()).execute();
  }

  private static ShippingRate buildRecord(ResultSet rs) {
    try {
      ShippingRate record = new ShippingRate();
      record.setId(rs.getLong("rate_id"));
      record.setCountryCode(rs.getString("country_code"));
      record.setRegion(rs.getString("region"));
      record.setPostalCode(rs.getString("postal_code"));
      record.setMinSubTotal(rs.getBigDecimal("min_subtotal"));
      record.setMinWeightOz(rs.getInt("min_weight_oz"));
      record.setShippingFee(rs.getBigDecimal("shipping_fee"));
      record.setHandlingFee(rs.getBigDecimal("handling_fee"));
      record.setShippingCode(rs.getString("shipping_code"));
      record.setShippingMethodId(rs.getInt("shipping_method"));
      record.setDisplayText(rs.getString("display_text"));
      record.setExcludeSkus(rs.getString("exclude_skus"));
      // joined tables
      record.setDescription(rs.getString("title"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
