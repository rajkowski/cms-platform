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

package com.simisinc.platform.presentation.widgets.cms;

import com.simisinc.platform.WidgetBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author matt rajkowski
 * @created 5/7/2022 8:30 AM
 */
class SearchFormWidgetTest extends WidgetBase {

  @Test
  void execute() {
    SearchFormWidget widget = new SearchFormWidget();
    widget.execute(widgetContext);
    Assertions.assertNotNull(pageRequest.getAttribute("placeholder"));
    Assertions.assertNotNull(pageRequest.getAttribute("expand"));
    Assertions.assertEquals(SearchFormWidget.JSP, widgetContext.getJsp());
  }
}