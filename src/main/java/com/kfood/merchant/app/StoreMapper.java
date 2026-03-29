package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.Store;

public final class StoreMapper {

  private StoreMapper() {}

  public static CreateStoreOutput toCreateOutput(Store store) {
    return new CreateStoreOutput(
        store.getId(), store.getSlug(), store.getStatus(), store.getCreatedAt());
  }

  public static StoreOutput toOutput(Store store) {
    return new StoreOutput(
        store.getId(),
        store.getName(),
        store.getSlug(),
        store.getCnpj(),
        store.getPhone(),
        store.getTimezone(),
        store.getStatus());
  }
}
