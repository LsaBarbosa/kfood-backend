package com.kfood.merchant.app;

import com.kfood.merchant.api.CreateStoreResponse;
import com.kfood.merchant.api.StoreResponse;
import com.kfood.merchant.infra.persistence.Store;

public final class StoreMapper {

  private StoreMapper() {}

  public static CreateStoreResponse toCreateResponse(Store store) {
    return new CreateStoreResponse(
        store.getId(), store.getSlug(), store.getStatus(), store.getCreatedAt());
  }

  public static StoreResponse toResponse(Store store) {
    return new StoreResponse(
        store.getId(),
        store.getName(),
        store.getSlug(),
        store.getCnpj(),
        store.getPhone(),
        store.getTimezone(),
        store.getStatus());
  }
}
