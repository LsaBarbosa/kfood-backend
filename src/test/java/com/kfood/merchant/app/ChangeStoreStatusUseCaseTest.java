package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeStoreStatusUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      mock(StoreActivationRequirementsService.class);
  private final ChangeStoreStatusUseCase changeStoreStatusUseCase =
      new ChangeStoreStatusUseCase(
          merchantCommandPort, currentTenantProvider, storeActivationRequirementsService);

  @Test
  void shouldActivateWhenRequirementsAreMet() {
    var storeId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.ACTIVE);
    var requirements = new StoreActivationRequirements(true, true, true);
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
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements)).thenReturn(output);

    var response = changeStoreStatusUseCase.execute(command);

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    verify(merchantCommandPort).changeStoreStatus(storeId, command, requirements);
  }

  @Test
  void shouldPropagateRequirementsFailure() {
    var storeId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.ACTIVE);
    var requirements = new StoreActivationRequirements(false, true, true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements))
        .thenThrow(
            new StoreActivationRequirementsNotMetException(requirements.missingRequirements()));

    assertThatThrownBy(() -> changeStoreStatusUseCase.execute(command))
        .isInstanceOf(StoreActivationRequirementsNotMetException.class)
        .hasMessageContaining("hoursConfigured");
  }

  @Test
  void shouldPropagateInvalidSetupTransition() {
    var storeId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.SETUP);
    var requirements = new StoreActivationRequirements(true, true, true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantCommandPort.changeStoreStatus(storeId, command, requirements))
        .thenThrow(
            new BusinessException(
                com.kfood.shared.exceptions.ErrorCode.VALIDATION_ERROR,
                "Changing status to SETUP is not allowed",
                org.springframework.http.HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> changeStoreStatusUseCase.execute(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Changing status to SETUP is not allowed");
  }
}
