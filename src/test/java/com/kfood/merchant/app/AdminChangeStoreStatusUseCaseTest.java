package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.audit.MerchantStoreAuditPort;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminChangeStoreStatusUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final MerchantStoreAuditPort merchantStoreAuditPort = mock(MerchantStoreAuditPort.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      mock(StoreActivationRequirementsService.class);
  private final AdminChangeStoreStatusUseCase adminChangeStoreStatusUseCase =
      new AdminChangeStoreStatusUseCase(
          merchantCommandPort,
          merchantQueryPort,
          merchantStoreAuditPort,
          currentAuthenticatedUserProvider,
          storeActivationRequirementsService);

  @Test
  void shouldApplyStatusChangeToExplicitTargetStoreId() {
    var targetStoreId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var command = new ChangeStoreStatusCommand(StoreStatus.SUSPENDED);
    var requirements = new StoreActivationRequirements(true, true, true);
    var currentStore =
        new StoreDetailsOutput(
            targetStoreId,
            "loja-alvo",
            "Loja Alvo",
            StoreStatus.ACTIVE,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);
    var output =
        new StoreDetailsOutput(
            targetStoreId,
            "loja-alvo",
            "Loja Alvo",
            StoreStatus.SUSPENDED,
            "21999990000",
            "America/Sao_Paulo",
            true,
            true);

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(storeActivationRequirementsService.evaluate(targetStoreId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(targetStoreId, requirements)).thenReturn(currentStore);
    when(merchantCommandPort.changeStoreStatus(targetStoreId, command, requirements))
        .thenReturn(output);

    var response = adminChangeStoreStatusUseCase.execute(targetStoreId, command);

    assertThat(response.status()).isEqualTo(StoreStatus.SUSPENDED);
    verify(currentAuthenticatedUserProvider).getRequiredUserId();
    verify(storeActivationRequirementsService).evaluate(targetStoreId);
    verify(merchantQueryPort).getStoreDetails(targetStoreId, requirements);
    verify(merchantCommandPort).changeStoreStatus(targetStoreId, command, requirements);
    verify(merchantStoreAuditPort)
        .recordStoreStatusChanged(
            targetStoreId, actorUserId, StoreStatus.ACTIVE, StoreStatus.SUSPENDED);
  }
}
