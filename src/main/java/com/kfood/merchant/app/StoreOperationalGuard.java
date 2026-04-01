package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreStatus;
import org.springframework.stereotype.Component;

@Component
public class StoreOperationalGuard {

  public void ensureStoreIsActive(java.util.UUID storeId, StoreStatus storeStatus) {
    if (storeStatus != StoreStatus.ACTIVE) {
      throw new StoreNotActiveException(storeId, storeStatus);
    }
  }

  public void ensureStoreIsNotSuspended(java.util.UUID storeId, StoreStatus storeStatus) {
    if (storeStatus == StoreStatus.SUSPENDED) {
      throw new StoreNotActiveException(storeId, storeStatus);
    }
  }
}
