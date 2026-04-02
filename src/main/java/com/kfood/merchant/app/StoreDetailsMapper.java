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
        store.category(),
        toAddressOutput(store.address()),
        requirements.hoursConfigured(),
        requirements.deliveryZonesConfigured());
  }

  private static StoreAddressOutput toAddressOutput(MerchantViews.StoreAddressView address) {
    return address == null
        ? null
        : new StoreAddressOutput(
            address.zipCode(),
            address.street(),
            address.number(),
            address.district(),
            address.city(),
            address.state());
  }
}
