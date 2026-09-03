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
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.ecommerce.SaveCustomerCommand;
import com.simisinc.platform.domain.model.ecommerce.Address;
import com.simisinc.platform.domain.model.ecommerce.Customer;

/**
 * Persists and retrieves customer objects
 *
 * @author matt rajkowski
 * @created 4/24/19 10:45 PM
 */
public class CustomerRepository {

  private static Log LOG = LogFactory.getLog(CustomerRepository.class);

  private static String TABLE_NAME = "customers";
  private static String[] PRIMARY_KEY = new String[] { "customer_id" };

  private static DataResult<Customer> query(CustomerSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("customer_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getEmail())) {
        select.AND("LOWER(email) = ?", specification.getEmail().toLowerCase());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("(LOWER(customer_unique_id) = LOWER(?) OR LOWER(customer_unique_id) LIKE LOWER(?))",
            specification.getUniqueId(), specification.getUniqueId() + "%");
      }
      if (StringUtils.isNotBlank(specification.getOrderNumber())) {
        select.AND("EXISTS (SELECT 1 FROM orders WHERE customers.customer_id = orders.customer_id AND LOWER(order_unique_id) = ?)",
            specification.getOrderNumber().toLowerCase());
      }
      if (StringUtils.isNotBlank(specification.getPhoneNumber())) {
        select.AND("phone_number = ?", specification.getPhoneNumber());
      }
      if (StringUtils.isNotBlank(specification.getName())) {
        select.AND(
            "(LOWER(concat_ws(' ', first_name, last_name)) LIKE LOWER(?) ESCAPE '!' OR LOWER(concat_ws(' ', shipping_first_name, shipping_last_name)) LIKE LOWER(?) ESCAPE '!')",
            "%" + specification.getName() + "%",
            "%" + specification.getName() + "%");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(CustomerRepository::buildRecord);
  }

  public static List<Customer> findAll(CustomerSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("customer_id");
    DataResult result = query(specification, constraints);
    return (List<Customer>) result.getRecords();
  }

  public static Customer findById(long customerId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("customer_id = ?", customerId)
        .returnRecord(CustomerRepository::buildRecord);
  }

  public static Customer save(Customer record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Customer add(Customer record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)

      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("customer_unique_id", record.getUniqueId())
          .FIELD("email", record.getEmail())
          .FIELD("first_name", record.getFirstName())
          .FIELD("last_name", record.getLastName())
          .FIELD("organization", record.getOrganization())
          .FIELD("barcode", record.getBarcode())
          /*
           * .FIELD("street_address", record.getStreet()) .FIELD("address_line_2",
           * record.getAddressLine2()) .FIELD("address_line_3",
           * record.getAddressLine3()) .FIELD("city", record.getCity())
           * .FIELD("state", record.getState()) .FIELD("country",
           * record.getCountry()) .FIELD("postal_code", record.getPostalCode())
           * .FIELD("county", record.getCounty())
           */
          .FIELD("phone_number", record.getPhoneNumber())
          .FIELD("tax_id", record.getTaxId())
          .FIELD("remote_customer_id", record.getRemoteCustomerId())
          .FIELD_UNLESS_MATCHES("created_by", record.getCreatedBy(), -1)
          .FIELD_UNLESS_MATCHES("modified_by", record.getModifiedBy(), -1);
      if (record.getBillingAddress() != null) {
        insert
            .FIELD("billing_first_name", record.getBillingAddress().getFirstName())
            .FIELD("billing_last_name", record.getBillingAddress().getLastName())
            .FIELD("billing_organization", record.getBillingAddress().getOrganization())
            .FIELD("billing_street_address", record.getBillingAddress().getStreet())
            .FIELD("billing_address_line_2", record.getBillingAddress().getAddressLine2())
            .FIELD("billing_address_line_3", record.getBillingAddress().getAddressLine3())
            .FIELD("billing_city", record.getBillingAddress().getCity())
            .FIELD("billing_state", record.getBillingAddress().getState())
            .FIELD("billing_country", record.getBillingAddress().getCountry())
            .FIELD("billing_postal_code", record.getBillingAddress().getPostalCode())
            .FIELD("billing_county", record.getBillingAddress().getCounty())
            .FIELD("billing_phone_number", record.getBillingAddress().getPhoneNumber());
      }
      if (record.getShippingAddress() != null) {
        insert
            .FIELD("shipping_first_name", record.getShippingAddress().getFirstName())
            .FIELD("shipping_last_name", record.getShippingAddress().getLastName())
            .FIELD("shipping_organization", record.getShippingAddress().getOrganization())
            .FIELD("shipping_street_address", record.getShippingAddress().getStreet())
            .FIELD("shipping_address_line_2", record.getShippingAddress().getAddressLine2())
            .FIELD("shipping_address_line_3", record.getShippingAddress().getAddressLine3())
            .FIELD("shipping_city", record.getShippingAddress().getCity())
            .FIELD("shipping_state", record.getShippingAddress().getState())
            .FIELD("shipping_country", record.getShippingAddress().getCountry())
            .FIELD("shipping_postal_code", record.getShippingAddress().getPostalCode())
            .FIELD("shipping_county", record.getShippingAddress().getCounty())
            .FIELD("shipping_phone_number", record.getShippingAddress().getPhoneNumber());
      }
      record.setId(insert.execute(connection));

      // Generate a new customer unique id
      LOG.debug("Updating customer unique id for id: " + record.getId());
      DB.UPDATE(TABLE_NAME)
          .SET("customer_unique_id", SaveCustomerCommand.generateUniqueId(record))
          .WHERE("customer_id = ?", record.getId())
          .execute(connection);

      if (record.getCartId() > 0) {
        // Update the cart with the customer id
        LOG.debug("Updating cart " + record.getCartId() + " with the customer id");
        DB.UPDATE("carts")
            .SET("customer_id", record.getId())
            .WHERE("cart_id = ?", record.getCartId())
            .execute(connection);
      }
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException | CheckDigitException se) {
      LOG.error("Exception: " + se.getMessage(), se);
    }
    return null;
  }

  public static Customer update(Customer record) {
    if (record.getId() == -1) {
      LOG.debug("Can't update customer -1");
      return null;
    }

    Update update = DB.UPDATE(TABLE_NAME)
        .SET("email", record.getEmail())
        .SET("first_name", record.getFirstName())
        .SET("last_name", record.getLastName())
        .SET("organization", record.getOrganization())
        .SET("barcode", record.getBarcode())
        /*
         * .SET("street_address", record.getStreet()) .SET("address_line_2",
         * record.getAddressLine2()) .SET("address_line_3",
         * record.getAddressLine3()) .SET("city", record.getCity())
         * .SET("state", record.getState()) .SET("country",
         * record.getCountry()) .SET("postal_code", record.getPostalCode())
         * .SET("county", record.getCounty())
         */
        .SET("phone_number", record.getPhoneNumber())
        .SET("tax_id", record.getTaxId())
        .SET("remote_customer_id", record.getRemoteCustomerId())
        .SET_UNLESS_MATCHES("modified_by", record.getModifiedBy(), -1)
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.getBillingAddress() != null) {
      LOG.debug("Updating the billing information...");
      update
          .SET("billing_first_name", record.getBillingAddress().getFirstName())
          .SET("billing_last_name", record.getBillingAddress().getLastName())
          .SET("billing_organization", record.getBillingAddress().getOrganization())
          .SET("billing_street_address", record.getBillingAddress().getStreet())
          .SET("billing_address_line_2", record.getBillingAddress().getAddressLine2())
          .SET("billing_address_line_3", record.getBillingAddress().getAddressLine3())
          .SET("billing_city", record.getBillingAddress().getCity())
          .SET("billing_state", record.getBillingAddress().getState())
          .SET("billing_country", record.getBillingAddress().getCountry())
          .SET("billing_postal_code", record.getBillingAddress().getPostalCode())
          .SET("billing_county", record.getBillingAddress().getCounty())
          .SET("billing_phone_number", record.getBillingAddress().getPhoneNumber());
    } else {
      LOG.debug("Resetting the billing information...");
      update
          .SET("billing_first_name", (String) null)
          .SET("billing_last_name", (String) null)
          .SET("billing_organization", (String) null)
          .SET("billing_street_address", (String) null)
          .SET("billing_address_line_2", (String) null)
          .SET("billing_address_line_3", (String) null)
          .SET("billing_city", (String) null)
          .SET("billing_state", (String) null)
          .SET("billing_country", (String) null)
          .SET("billing_postal_code", (String) null)
          .SET("billing_county", (String) null)
          .SET("billing_phone_number", (String) null);
    }
    if (record.getShippingAddress() != null) {
      LOG.debug("Updating the shipping information...");
      update
          .SET("shipping_first_name", record.getShippingAddress().getFirstName())
          .SET("shipping_last_name", record.getShippingAddress().getLastName())
          .SET("shipping_organization", record.getShippingAddress().getOrganization())
          .SET("shipping_street_address", record.getShippingAddress().getStreet())
          .SET("shipping_address_line_2", record.getShippingAddress().getAddressLine2())
          .SET("shipping_address_line_3", record.getShippingAddress().getAddressLine3())
          .SET("shipping_city", record.getShippingAddress().getCity())
          .SET("shipping_state", record.getShippingAddress().getState())
          .SET("shipping_country", record.getShippingAddress().getCountry())
          .SET("shipping_postal_code", record.getShippingAddress().getPostalCode())
          .SET("shipping_county", record.getShippingAddress().getCounty())
          .SET("shipping_phone_number", record.getShippingAddress().getPhoneNumber());
    } else {
      LOG.debug("Resetting the shipping information...");
      update
          .SET("shipping_first_name", (String) null)
          .SET("shipping_last_name", (String) null)
          .SET("shipping_organization", (String) null)
          .SET("shipping_street_address", (String) null)
          .SET("shipping_address_line_2", (String) null)
          .SET("shipping_address_line_3", (String) null)
          .SET("shipping_city", (String) null)
          .SET("shipping_state", (String) null)
          .SET("shipping_country", (String) null)
          .SET("shipping_postal_code", (String) null)
          .SET("shipping_county", (String) null)
          .SET("shipping_phone_number", (String) null);
    }
    update
        .WHERE("customer_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void updateContactInfo(Customer record) {
    if (record.getId() == -1) {
      LOG.debug("Can't update customer -1");
      return;
    }
    LOG.debug("Updating the customer record");
    DB.UPDATE(TABLE_NAME)
        .SET("first_name", record.getFirstName())
        .SET("last_name", record.getLastName())
        .SET("email", record.getEmail())
        .WHERE("customer_id = ?", record.getId())
        .execute();
  }

  private static Customer buildRecord(ResultSet rs) {
    try {
      Customer record = new Customer();
      Address billingAddress = new Address();
      Address shippingAddress = new Address();

      record.setId(rs.getLong("customer_id"));
      record.setUniqueId(rs.getString("customer_unique_id"));
      record.setEmail(rs.getString("email"));
      record.setFirstName(rs.getString("first_name"));
      record.setLastName(rs.getString("last_name"));
      record.setOrganization(rs.getString("organization"));
      record.setBarcode(rs.getString("barcode"));
      /*
       * record.setStreet(rs.getString("street_address"));
       * record.setAddressLine2(rs.getString("address_line_2"));
       * record.setAddressLine3(rs.getString("address_line_3"));
       * record.setCity(rs.getString("city")); record.setState(rs.getString("state"));
       * record.setCountry(rs.getString("country"));
       * record.setPostalCode(rs.getString("postal_code"));
       * record.setCounty(rs.getString("county"));
       */
      record.setPhoneNumber(rs.getString("phone_number"));
      billingAddress.setFirstName(rs.getString("billing_first_name"));
      billingAddress.setLastName(rs.getString("billing_last_name"));
      billingAddress.setOrganization(rs.getString("billing_organization"));
      billingAddress.setStreet(rs.getString("billing_street_address"));
      billingAddress.setAddressLine2(rs.getString("billing_address_line_2"));
      billingAddress.setAddressLine3(rs.getString("billing_address_line_3"));
      billingAddress.setCity(rs.getString("billing_city"));
      billingAddress.setState(rs.getString("billing_state"));
      billingAddress.setCountry(rs.getString("billing_country"));
      billingAddress.setPostalCode(rs.getString("billing_postal_code"));
      billingAddress.setCounty(rs.getString("billing_county"));
      billingAddress.setPhoneNumber(rs.getString("billing_phone_number"));
      shippingAddress.setFirstName(rs.getString("shipping_first_name"));
      shippingAddress.setLastName(rs.getString("shipping_last_name"));
      shippingAddress.setOrganization(rs.getString("shipping_organization"));
      shippingAddress.setStreet(rs.getString("shipping_street_address"));
      shippingAddress.setAddressLine2(rs.getString("shipping_address_line_2"));
      shippingAddress.setAddressLine3(rs.getString("shipping_address_line_3"));
      shippingAddress.setCity(rs.getString("shipping_city"));
      shippingAddress.setState(rs.getString("shipping_state"));
      shippingAddress.setCountry(rs.getString("shipping_country"));
      shippingAddress.setPostalCode(rs.getString("shipping_postal_code"));
      shippingAddress.setCounty(rs.getString("shipping_county"));
      shippingAddress.setPhoneNumber(rs.getString("shipping_phone_number"));
      record.setTaxId(rs.getString("tax_id"));
      record.setRemoteCustomerId(rs.getString("remote_customer_id"));
      record.setCurrency(rs.getString("currency"));
      record.setAccountBalance(rs.getBigDecimal("account_balance"));
      record.setTotalSpend(rs.getBigDecimal("total_spend"));
      record.setOrderCount(rs.getInt("order_count"));
      record.setDelinquent(rs.getBoolean("delinquent"));
      record.setDiscount(rs.getString("discount"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      // Update the aggregate
      record.setBillingAddress(billingAddress);
      record.setShippingAddress(shippingAddress);
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
