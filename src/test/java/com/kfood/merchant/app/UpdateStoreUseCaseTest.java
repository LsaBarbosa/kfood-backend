package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.api.UpdateStoreRequest;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateStoreUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final UpdateStoreUseCase updateStoreUseCase =
      new UpdateStoreUseCase(storeRepository, currentTenantProvider);

  @Test
  void shouldUpdateOnlyInformedFields() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request = new UpdateStoreRequest("Loja Premium", null, null, "21911112222", null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response = updateStoreUseCase.execute(request);

    assertThat(response.id()).isEqualTo(storeId);
    assertThat(response.name()).isEqualTo("Loja Premium");
    assertThat(response.phone()).isEqualTo("21911112222");
    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.cnpj()).isEqualTo("45.723.174/0001-10");
    assertThat(response.timezone()).isEqualTo("America/Sao_Paulo");
  }

  @Test
  void shouldUpdateSlugCnpjAndTimezoneWhenInformed() {
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
        new UpdateStoreRequest(null, "loja-do-bairro-premium", "31.662.365/0001-40", null, "UTC");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.existsBySlugAndIdNot("loja-do-bairro-premium", storeId)).thenReturn(false);
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response = updateStoreUseCase.execute(request);

    assertThat(response.slug()).isEqualTo("loja-do-bairro-premium");
    assertThat(response.cnpj()).isEqualTo("31.662.365/0001-40");
    assertThat(response.timezone()).isEqualTo("UTC");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var request = new UpdateStoreRequest("Novo nome", null, null, null, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateStoreUseCase.execute(request))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldRejectDuplicatedSlugOnUpdate() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request = new UpdateStoreRequest(null, "novo-slug", null, null, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.existsBySlugAndIdNot("novo-slug", storeId)).thenReturn(true);

    assertThatThrownBy(() -> updateStoreUseCase.execute(request))
        .isInstanceOf(StoreSlugAlreadyExistsException.class)
        .hasMessageContaining("novo-slug");
  }

  @Test
  void shouldAllowKeepingCurrentSlugWithoutConflictCheck() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var request = new UpdateStoreRequest("Loja Premium", "loja-do-bairro", null, null, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeRepository.saveAndFlush(store)).thenReturn(store);

    var response = updateStoreUseCase.execute(request);

    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.name()).isEqualTo("Loja Premium");
  }
}
