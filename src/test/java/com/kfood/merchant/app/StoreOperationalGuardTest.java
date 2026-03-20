package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreOperationalGuardTest {

  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();

  @Test
  void shouldAllowCriticalOperationWhenStoreIsActive() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.activate();

    assertThatCode(() -> storeOperationalGuard.ensureStoreIsActive(store))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldBlockCriticalOperationWhenStoreIsSuspended() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.activate();
    store.suspend();

    assertThatThrownBy(() -> storeOperationalGuard.ensureStoreIsActive(store))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SUSPENDED");
  }

  @Test
  void shouldBlockCriticalOperationWhenStoreIsSetup() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    assertThatThrownBy(() -> storeOperationalGuard.ensureStoreIsActive(store))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SETUP");
  }
}
