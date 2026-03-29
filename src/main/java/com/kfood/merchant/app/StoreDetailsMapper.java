package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.Store;

public final class StoreDetailsMapper {

  private StoreDetailsMapper() {}

  public static StoreDetailsOutput toOutput(Store store, StoreActivationRequirements requirements) {
    return new StoreDetailsOutput(
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
