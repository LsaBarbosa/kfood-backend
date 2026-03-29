package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.DeliveryZone;

public final class DeliveryZoneMapper {

  private DeliveryZoneMapper() {}

  public static DeliveryZoneOutput toOutput(DeliveryZone zone) {
    return new DeliveryZoneOutput(
        zone.getId(),
        zone.getZoneName(),
        zone.getFeeAmount(),
        zone.getMinOrderAmount(),
        zone.isActive());
  }
}
