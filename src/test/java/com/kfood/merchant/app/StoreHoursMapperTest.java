package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreHoursMapperTest {

  @Test
  void shouldMapStoreBusinessHourToResponse() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var entity =
        StoreBusinessHour.open(store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0));

    var response = StoreHoursMapper.toResponse(entity);

    assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(response.openTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(response.closeTime()).isEqualTo(LocalTime.of(22, 0));
    assertThat(response.isClosed()).isFalse();
  }
}
