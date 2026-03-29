package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeStoreStatusUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      mock(StoreActivationRequirementsService.class);
  private final ChangeStoreStatusUseCase changeStoreStatusUseCase =
      new ChangeStoreStatusUseCase(
          storeRepository, currentTenantProvider, storeActivationRequirementsService);

  @Test
  void shouldRemainInSetupWhenTermsWereNotAccepted() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, false));

    assertThatThrownBy(
            () ->
                changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.ACTIVE)))
        .isInstanceOf(StoreActivationRequirementsNotMetException.class)
        .hasMessageContaining("termsAccepted");

    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);
  }

  @Test
  void shouldRemainInSetupWhenHoursAreMissing() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(false, true, true));

    assertThatThrownBy(
            () ->
                changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.ACTIVE)))
        .isInstanceOf(StoreActivationRequirementsNotMetException.class)
        .hasMessageContaining("hoursConfigured");

    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);
  }

  @Test
  void shouldActivateWhenStoreIsComplete() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, true));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response =
        changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.ACTIVE));

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
  }

  @Test
  void shouldSuspendWhenStoreIsActive() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, true));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response =
        changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.SUSPENDED));

    assertThat(response.status()).isEqualTo(StoreStatus.SUSPENDED);
    assertThat(store.getStatus()).isEqualTo(StoreStatus.SUSPENDED);
  }

  @Test
  void shouldReactivateSuspendedStoreWhenRequirementsAreMet() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, true));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response =
        changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.ACTIVE));

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
  }

  @Test
  void shouldRejectInvalidTransition() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, true));

    assertThatThrownBy(
            () ->
                changeStoreStatusUseCase.execute(
                    new ChangeStoreStatusCommand(StoreStatus.SUSPENDED)))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Invalid store status transition");
  }

  @Test
  void shouldRejectChangingStatusBackToSetup() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, true));

    assertThatThrownBy(
            () -> changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.SETUP)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Changing status to SETUP is not allowed");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                changeStoreStatusUseCase.execute(new ChangeStoreStatusCommand(StoreStatus.ACTIVE)))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  private Store store(UUID id) {
    return new Store(
        id,
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }
}
