package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.domain.StoreStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreOperationalGuardTest {

  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();

  @Test
  void shouldAllowCriticalOperationWhenStoreIsActive() {
    var storeId = UUID.randomUUID();

    assertThatCode(() -> storeOperationalGuard.ensureStoreIsActive(storeId, StoreStatus.ACTIVE))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldBlockCriticalOperationWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();

    assertThatThrownBy(
            () -> storeOperationalGuard.ensureStoreIsActive(storeId, StoreStatus.SUSPENDED))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SUSPENDED");
  }

  @Test
  void shouldBlockCriticalOperationWhenStoreIsSetup() {
    var storeId = UUID.randomUUID();

    assertThatThrownBy(() -> storeOperationalGuard.ensureStoreIsActive(storeId, StoreStatus.SETUP))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SETUP");
  }
}
