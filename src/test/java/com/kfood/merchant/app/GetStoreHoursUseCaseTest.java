package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStoreHoursUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final GetStoreHoursUseCase getStoreHoursUseCase =
      new GetStoreHoursUseCase(storeRepository, storeBusinessHourRepository, currentTenantProvider);

  @Test
  void shouldReturnHoursOrderedByDayOfWeek() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.incrementHoursVersion();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeBusinessHourRepository.findByStoreId(storeId))
        .thenReturn(
            List.of(
                StoreBusinessHour.closed(store, DayOfWeek.SUNDAY),
                StoreBusinessHour.open(
                    store, DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                StoreBusinessHour.open(
                    store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0))));

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
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getStoreHoursUseCase.execute())
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }
}
