package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.StoreBusinessHour;

public final class StoreHoursMapper {

  private StoreHoursMapper() {}

  public static StoreHourOutput toOutput(StoreBusinessHour entity) {
    return new StoreHourOutput(
        entity.getDayOfWeek(), entity.getOpenTime(), entity.getCloseTime(), entity.isClosed());
  }
}
