package com.kfood.merchant.app;

import com.kfood.merchant.api.StoreDetailsResponse;
import com.kfood.merchant.infra.persistence.Store;

public final class StoreDetailsMapper {

  private StoreDetailsMapper() {}

  public static StoreDetailsResponse toResponse(
      Store store, StoreActivationRequirements requirements) {
    return new StoreDetailsResponse(
        store.getId(),
        store.getSlug(),
        store.getName(),
        store.getStatus(),
        store.getPhone(),
        store.getTimezone(),
        requirements.hoursConfigured(),
        requirements.deliveryZonesConfigured());
  }
}
