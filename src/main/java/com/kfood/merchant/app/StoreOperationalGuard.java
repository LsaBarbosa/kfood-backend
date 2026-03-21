package com.kfood.merchant.app;

import com.kfood.merchant.infra.persistence.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreOperationalGuard {

  public void ensureStoreIsActive(Store store) {
    if (!store.isActive()) {
      throw new StoreNotActiveException(store.getId(), store.getStatus());
    }
  }

  public void ensureStoreIsNotSuspended(Store store) {
    if (store.isSuspended()) {
      throw new StoreNotActiveException(store.getId(), store.getStatus());
    }
  }
}
