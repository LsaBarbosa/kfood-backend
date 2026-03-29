package com.kfood.merchant.app;

public final class StoreDetailsMapper {

  private StoreDetailsMapper() {}

  public static StoreDetailsOutput toOutput(
      MerchantViews.StoreView store, StoreActivationRequirements requirements) {
    return new StoreDetailsOutput(
        store.id(),
        store.slug(),
        store.name(),
        store.status(),
        store.phone(),
        store.timezone(),
        requirements.hoursConfigured(),
        requirements.deliveryZonesConfigured());
  }
}
