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

package com.simisinc.platform.presentation.widgets;

import java.lang.reflect.InvocationTargetException;

import com.simisinc.platform.presentation.controller.WidgetContext;

/**
 * Defines the contract for all widgets.
 *
 * @author matt rajkowski
 */
public interface Widget {

  WidgetContext execute(WidgetContext context);

  WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException;

  WidgetContext delete(WidgetContext context) throws InvocationTargetException, IllegalAccessException;
}
