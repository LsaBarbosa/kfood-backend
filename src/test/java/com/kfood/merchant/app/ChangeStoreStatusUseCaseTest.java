package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.audit.MerchantStoreAuditPort;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeStoreStatusUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final MerchantStoreAuditPort merchantStoreAuditPort = mock(MerchantStoreAuditPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      mock(StoreActivationRequirementsService.class);
  private final ChangeStoreStatusUseCase changeStoreStatusUseCase =
      new ChangeStoreStatusUseCase(
          merchantCommandPort,
          merchantQueryPort,
          merchantStoreAuditPort,
          currentTenantProvider,
          currentAuthenticatedUserProvider,
          storeActivationRequirementsService);

  @Test
  void shouldActivateWhenRequirementsAreMet() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.ACTIVE);
    var requirements = new StoreActivationRequirements(true, true, true);
    var currentStore =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.SETUP,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);
    var output =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.ACTIVE,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(storeId, requirements)).thenReturn(currentStore);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements)).thenReturn(output);

    var response = changeStoreStatusUseCase.execute(command);

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    verify(currentTenantProvider).getRequiredStoreId();
    verify(currentAuthenticatedUserProvider).getRequiredUserId();
    verify(storeActivationRequirementsService).evaluate(storeId);
    verify(merchantQueryPort).getStoreDetails(storeId, requirements);
    verify(merchantCommandPort).changeStoreStatus(storeId, command, requirements);
    verify(merchantStoreAuditPort)
        .recordStoreStatusChanged(storeId, actorUserId, currentStore.status(), output.status());
  }

  @Test
  void shouldSuspendActiveStore() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.SUSPENDED);
    var requirements = new StoreActivationRequirements(true, true, true);
    var currentStore =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.ACTIVE,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);
    var output =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.SUSPENDED,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(storeId, requirements)).thenReturn(currentStore);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements)).thenReturn(output);

    var response = changeStoreStatusUseCase.execute(command);

    assertThat(response.status()).isEqualTo(StoreStatus.SUSPENDED);
    verify(merchantStoreAuditPort)
        .recordStoreStatusChanged(storeId, actorUserId, StoreStatus.ACTIVE, StoreStatus.SUSPENDED);
  }

  @Test
  void shouldReactivateSuspendedStore() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.ACTIVE);
    var requirements = new StoreActivationRequirements(true, true, true);
    var currentStore =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.SUSPENDED,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);
    var output =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.ACTIVE,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(storeId, requirements)).thenReturn(currentStore);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements)).thenReturn(output);

    var response = changeStoreStatusUseCase.execute(command);

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    verify(merchantStoreAuditPort)
        .recordStoreStatusChanged(storeId, actorUserId, StoreStatus.SUSPENDED, StoreStatus.ACTIVE);
  }

  @Test
  void shouldPropagateRequirementsFailure() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.ACTIVE);
    var requirements = new StoreActivationRequirements(false, true, true);
    var currentStore =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.SETUP,
            "21999990000",
            "America/Sao_Paulo",
            false,
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(storeId, requirements)).thenReturn(currentStore);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements))
        .thenThrow(
            new StoreActivationRequirementsNotMetException(requirements.missingRequirements()));

    assertThatThrownBy(() -> changeStoreStatusUseCase.execute(command))
        .isInstanceOf(StoreActivationRequirementsNotMetException.class)
        .hasMessageContaining("hoursConfigured");
    verifyNoInteractions(merchantStoreAuditPort);
  }

  @Test
  void shouldPropagateInvalidSetupTransition() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.SETUP);
    var requirements = new StoreActivationRequirements(true, true, true);
    var currentStore =
        new StoreDetailsOutput(
            storeId,
            "loja-do-bairro",
            "Loja do Bairro",
            StoreStatus.ACTIVE,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(storeId, requirements)).thenReturn(currentStore);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements))
        .thenThrow(
            new BusinessException(
                com.kfood.shared.exceptions.ErrorCode.VALIDATION_ERROR,
                "Changing status to SETUP is not allowed",
                org.springframework.http.HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> changeStoreStatusUseCase.execute(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Changing status to SETUP is not allowed");
    verifyNoInteractions(merchantStoreAuditPort);
  }
}
