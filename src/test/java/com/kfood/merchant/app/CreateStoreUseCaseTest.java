package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateStoreUseCaseTest {

  private final MerchantCommandPort merchantCommandPort = mock(MerchantCommandPort.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final CreateStoreUseCase createStoreUseCase =
      new CreateStoreUseCase(merchantCommandPort, currentAuthenticatedUserProvider);

  @Test
  void shouldCreateStoreAndBindOwnerToNewStore() {
    var userId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var output =
        new CreateStoreOutput(
            UUID.randomUUID(),
            "loja-do-bairro",
            StoreStatus.SETUP,
            Instant.parse("2026-03-20T10:00:00Z"));

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(merchantCommandPort.createStore(userId, request)).thenReturn(output);

    var response = createStoreUseCase.execute(request);

    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.status()).isEqualTo(StoreStatus.SETUP);
    verify(merchantCommandPort).createStore(userId, request);
  }

  @Test
  void shouldRejectDuplicatedSlug() {
    var userId = UUID.randomUUID();
    var request =
        new CreateStoreCommand(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(merchantCommandPort.createStore(userId, request))
        .thenThrow(new StoreSlugAlreadyExistsException("loja-do-bairro"));

    assertThatThrownBy(() -> createStoreUseCase.execute(request))
        .isInstanceOf(StoreSlugAlreadyExistsException.class)
        .hasMessageContaining("loja-do-bairro");
  }
}
