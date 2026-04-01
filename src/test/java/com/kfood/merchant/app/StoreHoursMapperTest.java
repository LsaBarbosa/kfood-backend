package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class StoreHoursMapperTest {

  @Test
  void shouldMapStoreBusinessHourToOutput() {
    var entity =
        new MerchantViews.StoreHourView(
            DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false);

    var response = StoreHoursMapper.toOutput(entity);

    assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(response.openTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(response.closeTime()).isEqualTo(LocalTime.of(22, 0));
    assertThat(response.closed()).isFalse();
  }
}
