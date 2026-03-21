package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.api.StoreHourRequest;
import com.kfood.merchant.api.UpdateStoreHoursRequest;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateStoreHoursUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final UpdateStoreHoursUseCase updateStoreHoursUseCase =
      new UpdateStoreHoursUseCase(
          storeRepository, storeBusinessHourRepository, currentTenantProvider);

  @Test
  void shouldSaveValidWeeklyGrid() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(
                new StoreHourRequest(
                    DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false),
                new StoreHourRequest(
                    DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false),
                new StoreHourRequest(DayOfWeek.SUNDAY, null, null, true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response = updateStoreHoursUseCase.execute(request);

    verify(storeBusinessHourRepository).deleteAllByStoreId(storeId);
    verify(storeBusinessHourRepository).saveAll(anyList());
    verify(storeRepository).saveAndFlush(store);
    assertThat(response.updated()).isTrue();
    assertThat(response.hoursVersion()).isEqualTo(1);
  }

  @Test
  void shouldRejectWhenOpenTimeIsAfterCloseTime() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(
                new StoreHourRequest(
                    DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(10, 0), false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("openTime must be before closeTime");
  }

  @Test
  void shouldRejectDuplicatedDay() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(
                new StoreHourRequest(
                    DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false),
                new StoreHourRequest(DayOfWeek.MONDAY, null, null, true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("Duplicated dayOfWeek");
  }

  @Test
  void shouldRejectClosedDayWithDefinedTimes() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(
                new StoreHourRequest(
                    DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("Closed day must not define openTime or closeTime");
  }

  @Test
  void shouldRejectOpenDayWithoutTimes() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(new StoreHourRequest(DayOfWeek.MONDAY, null, null, false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("Open day must define openTime and closeTime");
  }

  @Test
  void shouldRejectClosedDayWhenOnlyCloseTimeIsDefined() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(new StoreHourRequest(DayOfWeek.SUNDAY, null, LocalTime.of(22, 0), true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("Closed day must not define openTime or closeTime");
  }

  @Test
  void shouldRejectOpenDayWhenOnlyCloseTimeIsDefined() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(new StoreHourRequest(DayOfWeek.MONDAY, null, LocalTime.of(22, 0), false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("Open day must define openTime and closeTime");
  }

  @Test
  void shouldRejectOpenDayWhenOnlyOpenTimeIsDefined() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request =
        new UpdateStoreHoursRequest(
            List.of(new StoreHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), null, false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(InvalidStoreHoursException.class)
        .hasMessageContaining("Open day must define openTime and closeTime");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var request =
        new UpdateStoreHoursRequest(
            List.of(new StoreHourRequest(DayOfWeek.MONDAY, null, null, false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateStoreHoursUseCase.execute(request))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }
}
