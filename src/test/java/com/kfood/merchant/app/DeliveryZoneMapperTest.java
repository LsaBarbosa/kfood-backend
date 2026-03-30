package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryZoneMapperTest {

  @Test
  void shouldMapZoneToOutput() {
    var zone =
        new MerchantViews.DeliveryZoneView(
            UUID.randomUUID(), "Centro", new BigDecimal("6.50"), new BigDecimal("25.00"), true);

    var response = DeliveryZoneMapper.toOutput(zone);

    assertThat(response.id()).isEqualTo(zone.id());
    assertThat(response.zoneName()).isEqualTo("Centro");
    assertThat(response.feeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.minOrderAmount()).isEqualByComparingTo("25.00");
    assertThat(response.active()).isTrue();
  }
}
