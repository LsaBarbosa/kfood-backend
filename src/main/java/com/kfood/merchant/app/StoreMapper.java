package com.kfood.merchant.app;

public final class StoreMapper {

  private StoreMapper() {}

  public static CreateStoreOutput toCreateOutput(MerchantViews.StoreView store) {
    return new CreateStoreOutput(store.id(), store.slug(), store.status(), store.createdAt());
  }

  public static StoreOutput toOutput(MerchantViews.StoreView store) {
    return new StoreOutput(
        store.id(),
        store.name(),
        store.slug(),
        store.cnpj(),
        store.phone(),
        store.timezone(),
        store.category(),
        toAddressOutput(store.address()),
        store.status());
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
