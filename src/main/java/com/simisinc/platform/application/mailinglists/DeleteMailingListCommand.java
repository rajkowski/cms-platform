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

package com.simisinc.platform.application.mailinglists;

import com.simisinc.platform.domain.model.mailinglists.MailingList;
import com.simisinc.platform.infrastructure.persistence.mailinglists.MailingListRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods to delete a mailing list
 *
 * @author matt rajkowski
 * @created 3/25/19 9:04 AM
 */
public class DeleteMailingListCommand {

  private static Log LOG = LogFactory.getLog(DeleteMailingListCommand.class);

  public static boolean delete(MailingList mailingListBean) {
    if (mailingListBean.getMemberCount() > 10) {
      return false;
    }
    return MailingListRepository.remove(mailingListBean);
  }

}
