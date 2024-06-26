/*
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

package com.simisinc.platform.application.ecommerce;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Checks if e-commerce is running in test or production mode; test mode will use sandbox payment settings and mark
 * orders as a test
 *
 * @author matt rajkowski
 * @created 11/11/19 5:56 PM
 */
public class EcommerceCommand {

  private static Log LOG = LogFactory.getLog(EcommerceCommand.class);

  public static boolean isProductionEnabled() {
    return ("true".equals(LoadSitePropertyCommand.loadByName("ecommerce.production", "false")));
  }
}
