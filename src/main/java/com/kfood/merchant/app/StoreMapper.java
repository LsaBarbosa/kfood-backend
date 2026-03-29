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
        store.status());
  }
}
