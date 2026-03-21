package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStoreDetailsUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreActivationRequirementsService storeActivationRequirementsService =
      mock(StoreActivationRequirementsService.class);
  private final GetStoreDetailsUseCase getStoreDetailsUseCase =
      new GetStoreDetailsUseCase(
          storeRepository, currentTenantProvider, storeActivationRequirementsService);

  @Test
  void shouldReturnStoreStatusAndConfigurationFlags() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.activate();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeActivationRequirementsService.evaluate(storeId))
        .thenReturn(new StoreActivationRequirements(true, true, true));

    var response = getStoreDetailsUseCase.execute();

    assertThat(response.status()).isEqualTo(StoreStatus.ACTIVE);
    assertThat(response.hoursConfigured()).isTrue();
    assertThat(response.deliveryZonesConfigured()).isTrue();
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getStoreDetailsUseCase.execute())
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }
}
