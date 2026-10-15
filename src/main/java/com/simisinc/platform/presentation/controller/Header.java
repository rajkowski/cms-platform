/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a header component in the platform's presentation layer.
 *
 * @author matt rajkowski
 * @created 1/17/21 10:30 AM
 */
public class Header {

  private String name;
  private String cssClass = null;
  private String title;
  private String htmlId = "platform-menu";
  private String cssStyle;
  private boolean enabled = true;
  private boolean sticky;
  private String scrollBehavior = "none";
  private int scrollThreshold = 100;

  private List<Section> sections = new ArrayList<Section>();
  private List<String> roles = new ArrayList<String>();
  private List<String> groups = new ArrayList<String>();

  public Header() {
  }

  public Header(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Section> getSections() {
    return sections;
  }

  public void setSections(List<Section> sections) {
    this.sections = sections;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  public String getCssClass() {
    return cssClass;
  }

  public void setCssClass(String cssClass) {
    this.cssClass = cssClass;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getHtmlId() {
    return htmlId;
  }

  public void setHtmlId(String htmlId) {
    this.htmlId = htmlId;
  }

  public String getCssStyle() {
    return cssStyle;
  }

  public void setCssStyle(String cssStyle) {
    this.cssStyle = cssStyle;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isSticky() {
    return sticky;
  }

  public void setSticky(boolean sticky) {
    this.sticky = sticky;
  }

  public String getScrollBehavior() {
    return scrollBehavior;
  }

  public void setScrollBehavior(String scrollBehavior) {
    this.scrollBehavior = scrollBehavior;
  }

  public int getScrollThreshold() {
    return scrollThreshold;
  }

  public void setScrollThreshold(int scrollThreshold) {
    this.scrollThreshold = scrollThreshold;
  }
}
