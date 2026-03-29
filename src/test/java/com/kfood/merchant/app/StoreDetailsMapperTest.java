package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.Store;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreDetailsMapperTest {

  @Test
  void shouldMapStoreDetailsOutput() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var requirements = new StoreActivationRequirements(true, false, false);

    var response = StoreDetailsMapper.toOutput(store, requirements);

    assertThat(response.id()).isEqualTo(store.getId());
    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.name()).isEqualTo("Loja do Bairro");
    assertThat(response.status()).isEqualTo(StoreStatus.SETUP);
    assertThat(response.phone()).isEqualTo("21999990000");
    assertThat(response.timezone()).isEqualTo("America/Sao_Paulo");
    assertThat(response.hoursConfigured()).isTrue();
    assertThat(response.deliveryZonesConfigured()).isFalse();
  }
}
