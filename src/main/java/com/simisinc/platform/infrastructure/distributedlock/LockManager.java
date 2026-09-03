/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2023 SimIS Inc. (https://www.simiscms.com)
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
package com.simisinc.platform.infrastructure.distributedlock;

import java.time.Duration;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;

/**
 * A distributed lock implementation
 *
 * @author matt rajkowski
 * @created 3/26/23 5:00 PM
 */
public class LockManager {

  private static Log LOG = LogFactory.getLog(LockManager.class);

  private static String TABLE_NAME = "distributed_lock";

  public static String lock(String name, Duration duration) {

    String uuid = UUID.randomUUID().toString();

    // INSERT or UPDATE
    var insert = (DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", name)
        .FIELD("locked_at = CURRENT_TIMESTAMP")
        .FIELD("lock_until = CURRENT_TIMESTAMP - INTERVAL '10 SECONDS' + INTERVAL '" + duration.toString() + "'")
        .FIELD("uuid", uuid)
        .ON_CONFLICT("name")
        .DO_UPDATE()
        .SET("locked_at = EXCLUDED.locked_at")
        .SET("lock_until = EXCLUDED.lock_until")
        .SET("uuid = EXCLUDED.uuid")
        .WHERE("distributed_lock.name = EXCLUDED.name AND CURRENT_TIMESTAMP >= distributed_lock.lock_until"));

    if (insert.execute() > 0) {
      LOG.debug("Lock succeeded: " + name);
      return uuid;
    }
    return null;
  }

  public static boolean unlock(String name, String uuid) {
    return DB.UPDATE(TABLE_NAME)
        .SET("lock_until = CURRENT_TIMESTAMP")
        .WHERE("name = ?", name)
        .AND("uuid = ?", uuid)
        .execute();
  }
}
