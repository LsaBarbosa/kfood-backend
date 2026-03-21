package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.CreateCatalogCategoryRequest;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateCatalogCategoryUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final CreateCatalogCategoryUseCase createCatalogCategoryUseCase =
      new CreateCatalogCategoryUseCase(
          storeRepository, catalogCategoryRepository, currentTenantProvider, storeOperationalGuard);

  @Test
  void shouldCreateCategory() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);
    var request = new CreateCatalogCategoryRequest(" Pizzas ", 10);
    var savedCategory = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.existsByStoreIdAndName(storeId, "Pizzas")).thenReturn(false);
    when(catalogCategoryRepository.saveAndFlush(any(CatalogCategory.class)))
        .thenReturn(savedCategory);

    var response = createCatalogCategoryUseCase.execute(request);

    assertThat(response.id()).isEqualTo(savedCategory.getId());
    assertThat(response.name()).isEqualTo("Pizzas");
    assertThat(response.sortOrder()).isEqualTo(10);
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldRejectDuplicateNameWithinStore() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);
    var request = new CreateCatalogCategoryRequest("Pizzas", 10);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.existsByStoreIdAndName(storeId, "Pizzas")).thenReturn(true);

    assertThatThrownBy(() -> createCatalogCategoryUseCase.execute(request))
        .isInstanceOf(CatalogCategoryAlreadyExistsException.class)
        .hasMessageContaining("Pizzas");
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var request = new CreateCatalogCategoryRequest("Pizzas", 10);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createCatalogCategoryUseCase.execute(request))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldBlockCreationWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();
    var request = new CreateCatalogCategoryRequest("Pizzas", 10);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> createCatalogCategoryUseCase.execute(request))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SUSPENDED");
  }

  private Store store(UUID storeId) {
    return new Store(
        storeId,
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }
}
