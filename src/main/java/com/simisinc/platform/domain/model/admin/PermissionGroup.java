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
package com.simisinc.platform.domain.model.admin;

import java.util.ArrayList;
import java.util.List;

import com.simisinc.platform.domain.model.Entity;

/**
 * Represents a named permission group that associates a Cedar policy with
 * one or more widget or service component classes.
 *
 * @author matt rajkowski
 * @created 3/6/26 8:00 AM
 */
public class PermissionGroup extends Entity {

  private long id = -1L;
  private String name;
  private String code;
  private String cedarPolicyText;
  private List<String> memberClassNames = new ArrayList<>();
  private List<String> memberTypes = new ArrayList<>();
  private boolean enabled = true;

  public PermissionGroup() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getCedarPolicyText() {
    return cedarPolicyText;
  }

  public void setCedarPolicyText(String cedarPolicyText) {
    this.cedarPolicyText = cedarPolicyText;
  }

  public List<String> getMemberClassNames() {
    return memberClassNames;
  }

  public void setMemberClassNames(List<String> memberClassNames) {
    this.memberClassNames = memberClassNames;
  }

  public void addMemberClassName(String className, String type) {
    memberClassNames.add(className);
    memberTypes.add(type);
  }

  public List<String> getMemberTypes() {
    return memberTypes;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
