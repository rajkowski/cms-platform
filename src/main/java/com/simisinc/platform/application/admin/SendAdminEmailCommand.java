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

package com.simisinc.platform.application.admin;

import com.simisinc.platform.application.email.EmailCommand;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.RoleRepository;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.ImageHtmlEmail;

import java.util.List;

/**
 * Sends emails to admins
 *
 * @author matt rajkowski
 * @created 7/13/18 8:31 AM
 */
public class SendAdminEmailCommand {

  private static Log LOG = LogFactory.getLog(SendAdminEmailCommand.class);

  public static void sendMessage(String subject, String html, String text) {

    // Send to all admins
    Role role = RoleRepository.findByCode("admin");
    if (role == null) {
      return;
    }
    List<User> userList = UserRepository.findAllByRole(role);
    if (userList.isEmpty()) {
      return;
    }

    try {
      ImageHtmlEmail email = EmailCommand.prepareNewEmail();
      for (User user : userList) {
        if (user.getEmail().contains("@")) {
          LOG.debug("Sending admin email to: " + user.getEmail() + " " + user.getFullName());
          email.addTo(user.getEmail(), user.getFullName());
        }
      }
      email.setSubject(subject);
      email.setHtmlMsg(html);
      email.setTextMsg(text);
      email.send();
    } catch (Exception e) {
      LOG.error("sendAdminEmail could not send mail", e);
    }

  }

}
