package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.infra.persistence.DeliveryZone;
import com.kfood.merchant.infra.persistence.Store;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryZoneMapperTest {

  @Test
  void shouldMapZoneToResponse() {
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            new Store(
                UUID.randomUUID(),
                "Loja do Bairro",
                "loja-do-bairro",
                "45.723.174/0001-10",
                "21999990000",
                "America/Sao_Paulo"),
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    var response = DeliveryZoneMapper.toResponse(zone);

    assertThat(response.id()).isEqualTo(zone.getId());
    assertThat(response.zoneName()).isEqualTo("Centro");
    assertThat(response.feeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.minOrderAmount()).isEqualByComparingTo("25.00");
    assertThat(response.active()).isTrue();
  }
}
