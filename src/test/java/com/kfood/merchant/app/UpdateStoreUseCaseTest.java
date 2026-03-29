package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateStoreUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final UpdateStoreUseCase updateStoreUseCase =
      new UpdateStoreUseCase(merchantCommandPort, currentTenantProvider);

  @Test
  void shouldUpdateOnlyInformedFields() {
    var storeId = UUID.randomUUID();
    var request = new UpdateStoreCommand("Loja Premium", null, null, "21911112222", null);
    var output =
        new StoreOutput(
            storeId,
            "Loja Premium",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21911112222",
            "America/Sao_Paulo",
            StoreStatus.SETUP);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.updateStore(storeId, request)).thenReturn(output);

    var response = updateStoreUseCase.execute(request);

    assertThat(response.name()).isEqualTo("Loja Premium");
    assertThat(response.phone()).isEqualTo("21911112222");
    verify(merchantCommandPort).updateStore(storeId, request);
  }

  @Test
  void shouldPropagateDuplicatedSlugOnUpdate() {
    var storeId = UUID.randomUUID();
    var request = new UpdateStoreCommand(null, "novo-slug", null, null, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(merchantCommandPort.updateStore(storeId, request))
        .thenThrow(new StoreSlugAlreadyExistsException("novo-slug"));

    assertThatThrownBy(() -> updateStoreUseCase.execute(request))
        .isInstanceOf(StoreSlugAlreadyExistsException.class)
        .hasMessageContaining("novo-slug");
  }
}
