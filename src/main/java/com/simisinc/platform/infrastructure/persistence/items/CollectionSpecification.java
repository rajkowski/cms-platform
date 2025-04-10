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

package com.simisinc.platform.infrastructure.persistence.items;

/**
 * Properties for querying objects from the collection repository
 *
 * @author matt rajkowski
 * @created 1/22/19 12:12 PM
 */
public class CollectionSpecification {

  private Long id = -1L;
  private String uniqueId = null;
  private String name = null;
  private Long forUserId = -1L;

  public CollectionSpecification() {
  }

  public CollectionSpecification(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getForUserId() {
    return forUserId;
  }

  public void setForUserId(Long forUserId) {
    this.forUserId = forUserId;
  }
}
