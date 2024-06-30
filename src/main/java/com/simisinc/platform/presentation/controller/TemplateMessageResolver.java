/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

import com.simisinc.platform.application.UserCommand;

/**
 * Resolves Thymeleaf messages
 *
 * @author matt rajkowski
 * @created 1/20/24 2:02 PM
 */
public class TemplateMessageResolver implements IMessageResolver {

  public static final Logger LOG = LoggerFactory.getLogger(TemplateMessageResolver.class);

  public TemplateMessageResolver() {
    super();
  }

  /** {@inheritDoc} */
  @Override
  public String resolveMessage(ITemplateContext iTemplateContext, Class<?> aClass, String message, Object[] objects) {
    LOG.debug("resolveMessage: " + message);
    if ("user.name".equals(message)) {
      if (objects != null && objects.length == 1) {
        long userId = Long.parseLong(objects[0].toString());
        return UserCommand.name(userId);
      }
    }
    return "";
  }

  /** {@inheritDoc} */
  @Override
  public String createAbsentMessageRepresentation(ITemplateContext iTemplateContext, Class<?> aClass, String s,
      Object[] objects) {
    return String.format("key:{}", s);
  }

  /** {@inheritDoc} */
  public Integer getOrder() {
    return 0;
  }

  /** {@inheritDoc} */
  public String getName() {
    return "resolver";
  }
}