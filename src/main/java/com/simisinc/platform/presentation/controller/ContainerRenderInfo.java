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

package com.simisinc.platform.presentation.controller;

import java.util.List;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 1/18/21 8:32 AM
 */
public interface ContainerRenderInfo {

  String getName();

  void setName(String name);

  boolean hasWidgets();

  void setHasWidgets(boolean hasWidgets);

  List<SectionRenderInfo> getSectionRenderInfoList();

  void addSection(SectionRenderInfo sectionRenderInfo);

  String getPagePath();

  void setPagePath(String pagePath);

  String getCssClass();

  String getTargetWidget();
}
