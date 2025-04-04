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

package com.simisinc.platform.domain.model.cms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 2/17/2020 9:05 PM
 */
public class AccordionSection implements Serializable {

  static final long serialVersionUID = -8484048371911908893L;

  private String title = null;
  private String uniqueId = null;
  private List<String> labelsList = new ArrayList<>();
  private List<String> contentList = new ArrayList<>();
  private List<String> uniqueIdList = new ArrayList<>();

  public AccordionSection() {
  }

  public AccordionSection(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public List<String> getLabelsList() {
    return labelsList;
  }

  public void setLabelsList(List<String> labelsList) {
    this.labelsList = labelsList;
  }

  public List<String> getContentList() {
    return contentList;
  }

  public void setContentList(List<String> contentList) {
    this.contentList = contentList;
  }

  public List<String> getUniqueIdList() {
    return uniqueIdList;
  }

  public void setUniqueIdList(List<String> uniqueIdList) {
    this.uniqueIdList = uniqueIdList;
  }
}
