/*
 * Copyright 2026 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.application;

import org.apache.commons.lang3.StringUtils;

/**
 * Checks a user-entered value against the width of the column it is about to be written to
 */
public class FieldLengthCommand {

  private FieldLengthCommand() {
    // static helper
  }

  /**
   * Whether the value would be too long for its column once trimmed
   *
   * @param value the submitted value, which may be null
   * @param maxLength the column width
   * @return true when the value cannot be stored
   */
  public static boolean exceedsLimit(String value, int maxLength) {
    return StringUtils.trimToEmpty(value).length() > maxLength;
  }

  /**
   * The message shown when a value is too long, phrased so the admin can act on it: which field,
   * and what the limit actually is
   *
   * @param fieldLabel the field as the form labels it, e.g. "A name"
   * @param maxLength the column width
   */
  public static String tooLongMessage(String fieldLabel, int maxLength) {
    return fieldLabel + " can be up to " + maxLength + " characters";
  }

  /**
   * Appends the too-long message to a command's running validation message when the value does not
   * fit, using that command's own separator so the result reads as one sentence rather than two
   * messages run together
   *
   * @param errorMessages the command's accumulating validation message
   * @param separator the separator this command already uses between messages
   * @param fieldLabel the field as the form labels it, e.g. "A name"
   * @param value the submitted value
   * @param maxLength the column width
   * @return true when a message was appended
   */
  public static boolean appendIfTooLong(StringBuilder errorMessages, String separator, String fieldLabel,
      String value, int maxLength) {
    if (!exceedsLimit(value, maxLength)) {
      return false;
    }
    if (errorMessages.length() > 0) {
      errorMessages.append(separator);
    }
    errorMessages.append(tooLongMessage(fieldLabel, maxLength));
    return true;
  }
}
