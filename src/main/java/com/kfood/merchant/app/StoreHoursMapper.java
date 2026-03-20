package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreHourResponse;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;

public final class StoreHoursMapper {

  private StoreHoursMapper() {}

  public static StoreHourResponse toResponse(StoreBusinessHour entity) {
    return new StoreHourResponse(
        entity.getDayOfWeek(), entity.getOpenTime(), entity.getCloseTime(), entity.isClosed());
  }
}
