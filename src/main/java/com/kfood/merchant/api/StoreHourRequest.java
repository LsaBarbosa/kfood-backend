package com.kfood.merchant.api;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record StoreHourRequest(
    @NotNull DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, Boolean isClosed) {

  public boolean closed() {
    return Boolean.TRUE.equals(isClosed);
  }
}
