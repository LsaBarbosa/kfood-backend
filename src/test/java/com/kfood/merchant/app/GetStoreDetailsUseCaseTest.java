package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantQueryPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStoreDetailsUseCaseTest {

  private final MerchantQueryPort merchantQueryPort = mock(MerchantQueryPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      mock(StoreActivationRequirementsService.class);
  private final GetStoreDetailsUseCase getStoreDetailsUseCase =
      new GetStoreDetailsUseCase(
          merchantQueryPort, currentTenantProvider, storeActivationRequirementsService);

  @Test
  void shouldReturnStoreStatusAndConfigurationFlags() {
    var storeId = UUID.randomUUID();
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
    when(merchantQueryPort.getStoreDetails(storeId, requirements)).thenReturn(output);

    var response = getStoreDetailsUseCase.execute();

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(response.hoursConfigured()).isTrue();
    assertThat(response.deliveryZonesConfigured()).isTrue();
    verify(merchantQueryPort).getStoreDetails(storeId, requirements);
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var requirements = new StoreActivationRequirements(true, true, true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeActivationRequirementsService.evaluate(storeId)).thenReturn(requirements);
    when(merchantQueryPort.getStoreDetails(storeId, requirements))
        .thenThrow(new StoreNotFoundException(storeId));

    assertThatThrownBy(() -> getStoreDetailsUseCase.execute())
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }
}
