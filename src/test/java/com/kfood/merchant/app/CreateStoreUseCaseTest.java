package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.api.CreateStoreRequest;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateStoreUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CreateStoreUseCase createStoreUseCase = new CreateStoreUseCase(storeRepository);

  @Test
  void shouldCreateStoreSuccessfully() throws Exception {
    var request =
        new CreateStoreRequest(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    var persistedStore =
        new Store(
            UUID.randomUUID(),
            request.name(),
            request.slug(),
            request.cnpj(),
            request.phone(),
            request.timezone());
    setAuditableField(persistedStore, "createdAt", Instant.parse("2026-03-20T10:00:00Z"));

    when(storeRepository.existsBySlug(request.slug())).thenReturn(false);
    when(storeRepository.saveAndFlush(any(Store.class))).thenReturn(persistedStore);

    var response = createStoreUseCase.execute(request);

    assertThat(response.id()).isEqualTo(persistedStore.getId());
    assertThat(response.slug()).isEqualTo("loja-do-bairro");
    assertThat(response.status()).isEqualTo(StoreStatus.SETUP);
    assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-03-20T10:00:00Z"));
  }

  @Test
  void shouldRejectDuplicatedSlug() {
    var request =
        new CreateStoreRequest(
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(storeRepository.existsBySlug("loja-do-bairro")).thenReturn(true);

    assertThatThrownBy(() -> createStoreUseCase.execute(request))
        .isInstanceOf(StoreSlugAlreadyExistsException.class)
        .hasMessageContaining("loja-do-bairro");
  }

  private void setAuditableField(Store store, String fieldName, Instant value) throws Exception {
    Field field = store.getClass().getSuperclass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(store, value);
  }
}
