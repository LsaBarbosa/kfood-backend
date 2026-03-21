package com.kfood.merchant.app;

import com.kfood.merchant.api.PublicDeliveryZoneResponse;
import com.kfood.merchant.api.PublicStoreHourResponse;
import com.kfood.merchant.api.PublicStoreResponse;
import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import java.util.List;

public final class PublicStoreMapper {

  private PublicStoreMapper() {}

  public static PublicStoreResponse toResponse(
      Store store,
      List<PublicStoreHourResponse> hours,
      List<PublicDeliveryZoneResponse> deliveryZones) {
    return new PublicStoreResponse(
        store.getSlug(),
        store.getName(),
        store.getStatus(),
        store.getPhone(),
        hours,
        deliveryZones);
  }

  public static PublicStoreHourResponse toHourResponse(StoreBusinessHour hour) {
    return new PublicStoreHourResponse(
        hour.getDayOfWeek(), hour.getOpenTime(), hour.getCloseTime(), hour.isClosed());
  }

  public static PublicDeliveryZoneResponse toDeliveryZoneResponse(DeliveryZone zone) {
    return new PublicDeliveryZoneResponse(
        zone.getZoneName(), zone.getFeeAmount(), zone.getMinOrderAmount());
  }
}
