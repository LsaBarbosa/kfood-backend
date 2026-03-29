package com.kfood.merchant.app;

public final class DeliveryZoneMapper {

  private DeliveryZoneMapper() {}

  public static DeliveryZoneOutput toOutput(MerchantViews.DeliveryZoneView zone) {
    return new DeliveryZoneOutput(
        zone.id(), zone.zoneName(), zone.feeAmount(), zone.minOrderAmount(), zone.active());
  }
}
