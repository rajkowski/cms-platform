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

package com.simisinc.platform.domain.model.medicine;

import com.simisinc.platform.application.cms.TimeCommand;
import com.simisinc.platform.domain.model.Entity;

import java.time.ZoneId;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 9/10/18 4:36 PM
 */
public class MedicineTime extends Entity {

  private Long id = -1L;

  private long scheduleId = -1;
  private long medicineId = -1;
  private int hour = -1;
  private int minute = -1;
  private int quantity = -1;

  public MedicineTime() {
  }

  public MedicineTime(String time, int quantity, ZoneId clientTimezone) {
    // Set the hour/minute
    int[] adjustedHourMinute = TimeCommand.adjustHoursMinutesClientToServer(time, clientTimezone);
    hour = adjustedHourMinute[0];
    minute = adjustedHourMinute[1];

    // Determine the quantity
    this.quantity = quantity;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public long getMedicineId() {
    return medicineId;
  }

  public void setMedicineId(long medicineId) {
    this.medicineId = medicineId;
  }

  public int getHour() {
    return hour;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public int getMinute() {
    return minute;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
