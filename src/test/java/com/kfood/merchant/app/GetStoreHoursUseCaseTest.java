package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStoreHoursUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetStoreHoursUseCase getStoreHoursUseCase =
      new GetStoreHoursUseCase(merchantQueryPort, currentTenantProvider);

  @Test
  void shouldReturnHoursOrderedByDayOfWeek() {
    var storeId = UUID.randomUUID();
    var output =
        new StoreHoursOutput(
            1,
            List.of(
                new StoreHourOutput(
                    DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false),
                new StoreHourOutput(
                    DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false),
                new StoreHourOutput(DayOfWeek.SUNDAY, null, null, true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.getStoreHours(storeId)).thenReturn(output);

    var response = getStoreHoursUseCase.execute();

    assertThat(response.hoursVersion()).isEqualTo(1);
    assertThat(response.hours())
        .extracting(item -> item.dayOfWeek().name())
        .containsExactly("MONDAY", "WEDNESDAY", "SUNDAY");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantQueryPort.getStoreHours(storeId)).thenThrow(new StoreNotFoundException(storeId));

    assertThatThrownBy(() -> getStoreHoursUseCase.execute())
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }
}
