package com.kfood.merchant.app;

public final class StoreHoursMapper {

  private StoreHoursMapper() {}

  public static StoreHourOutput toOutput(MerchantViews.StoreHourView entity) {
    return new StoreHourOutput(
        entity.dayOfWeek(), entity.openTime(), entity.closeTime(), entity.closed());
  }
}
