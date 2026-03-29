package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateStoreHoursUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final UpdateStoreHoursUseCase updateStoreHoursUseCase =
      new UpdateStoreHoursUseCase(merchantCommandPort, currentTenantProvider);

  @Test
  void shouldDelegateValidWeeklyGrid() {
    var storeId = UUID.randomUUID();
    var command =
        new UpdateStoreHoursCommand(
            List.of(
                new StoreHourCommand(
                    DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false),
                new StoreHourCommand(DayOfWeek.SUNDAY, null, null, true)));
    var output = new UpdateStoreHoursOutput(true, 1);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.updateStoreHours(storeId, command)).thenReturn(output);

    var response = updateStoreHoursUseCase.execute(command);

    assertThat(response.updated()).isTrue();
    assertThat(response.hoursVersion()).isEqualTo(1);
    verify(merchantCommandPort).updateStoreHours(storeId, command);
  }

  @Test
  void shouldPropagateValidationErrorsFromCommandPort() {
    var storeId = UUID.randomUUID();
    var command =
        new UpdateStoreHoursCommand(
            List.of(
                new StoreHourCommand(
                    DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(10, 0), false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.updateStoreHours(storeId, command))
        .thenThrow(new InvalidStoreHoursException("openTime must be before closeTime"));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(command))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("openTime must be before closeTime");
  }
}
