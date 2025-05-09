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

package com.simisinc.platform.infrastructure.persistence.cms;

/**
 * Properties for querying objects from the table of contents repository
 *
 * @author matt rajkowski
 * @created 12/7/18 4:43 PM
 */
public class TableOfContentsSpecification {

  private long id = -1L;
  private String tocUniqueId = null;

  public TableOfContentsSpecification() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getTocUniqueId() {
    return tocUniqueId;
  }

  public void setTocUniqueId(String tocUniqueId) {
    this.tocUniqueId = tocUniqueId;
  }
}
