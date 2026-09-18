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

package com.simisinc.platform.infrastructure.database.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.items.Item;

/**
 * Creates HTML versions of the text for editing
 *
 * @author matt rajkowski
 * @created 9/14/2026 7:25 AM
 */
public class V20260914_1001__item_summary_text extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection connection = context.getConnection();

    // For each items.summary database record, keep the original text in summary_text and create the
    // escaped HTML value in summary.

    final int PAGE_SIZE = 100;
    int itemsProcessed = 0;

    String selectSql = "SELECT item_id, summary FROM items WHERE summary IS NOT NULL ORDER BY item_id LIMIT ? OFFSET ?";
    String updateSql = "UPDATE items SET summary_text = ?, summary = ? WHERE item_id = ?";

    List<Item> items = new ArrayList<>();

    long offset = 0;
    while (true) {

      // Find items to migrate...
      try (PreparedStatement selectPst = connection.prepareStatement(selectSql)) {
        selectPst.setInt(1, PAGE_SIZE);
        selectPst.setLong(2, offset);
        try (ResultSet rs = selectPst.executeQuery()) {
          while (rs.next()) {
            Item item = new Item();
            item.setId(rs.getLong("item_id"));
            item.setSummary(rs.getString("summary"));
            items.add(item);
          }
        }
      }

      if (items.isEmpty()) {
        break;
      }

      // For each items...
      try (PreparedStatement updatePst = connection.prepareStatement(updateSql)) {
        for (Item item : items) {
          updatePst.setString(1, item.getSummary());
          updatePst.setString(2, HtmlCommand.textToHtml(item.getSummary()));
          updatePst.setLong(3, item.getId());
          updatePst.execute();
        }
      }
      itemsProcessed += items.size();

      items.clear();
      offset += PAGE_SIZE;
    }

    System.out.println("Items processed: " + itemsProcessed);
  }
}
