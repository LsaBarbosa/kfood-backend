package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.StoreCategory;
import com.kfood.merchant.domain.StoreStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreDetailsMapperTest {

  @Test
  void shouldMapStoreDetailsOutput() {
    var store =
        new MerchantViews.StoreView(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo",
            StoreCategory.PIZZARIA,
            new MerchantViews.StoreAddressView(
                "25000000", "Rua Central", "100", "Centro", "Mage", "RJ"),
            StoreStatus.SETUP,
            Instant.parse("2026-03-20T10:00:00Z"));
    var requirements = new StoreActivationRequirements(true, true, true, false, false);

    var response = StoreDetailsMapper.toOutput(store, requirements);

    assertThat(response.id()).isEqualTo(store.id());
    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.name()).isEqualTo("Loja do Bairro");
    assertThat(response.status()).isEqualTo(StoreStatus.SETUP);
    assertThat(response.phone()).isEqualTo("21999990000");
    assertThat(response.timezone()).isEqualTo("America/Sao_Paulo");
    assertThat(response.category()).isEqualTo(StoreCategory.PIZZARIA);
    assertThat(response.address().zipCode()).isEqualTo("25000000");
    assertThat(response.hoursConfigured()).isTrue();
    assertThat(response.deliveryZonesConfigured()).isFalse();
  }

  @Test
  void shouldMapStoreDetailsOutputWithoutAddress() {
    var store =
        new MerchantViews.StoreView(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo",
            StoreCategory.PIZZARIA,
            null,
            StoreStatus.SETUP,
            Instant.parse("2026-03-20T10:00:00Z"));

    var response =
        StoreDetailsMapper.toOutput(
            store, new StoreActivationRequirements(true, true, true, true, true));

    assertThat(response.address()).isNull();
  }
}
