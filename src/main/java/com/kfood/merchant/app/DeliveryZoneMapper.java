package com.kfood.merchant.app;

import com.kfood.merchant.api.DeliveryZoneResponse;
import com.kfood.merchant.infra.persistence.DeliveryZone;

public final class DeliveryZoneMapper {

  private DeliveryZoneMapper() {}

  public static DeliveryZoneResponse toResponse(DeliveryZone zone) {
    return new DeliveryZoneResponse(
        zone.getId(),
        zone.getZoneName(),
        zone.getFeeAmount(),
        zone.getMinOrderAmount(),
        zone.isActive());
  }
}
