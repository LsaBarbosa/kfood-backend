package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.UpdateCatalogCategoryRequest;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateCatalogCategoryUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final UpdateCatalogCategoryUseCase updateCatalogCategoryUseCase =
      new UpdateCatalogCategoryUseCase(
          storeRepository, catalogCategoryRepository, currentTenantProvider, storeOperationalGuard);

  @Test
  void shouldUpdateCategory() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var category = new CatalogCategory(categoryId, store, "Pizzas", 10, true);
    var request = new UpdateCatalogCategoryRequest(" Bebidas ", 20);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.of(category));
    when(catalogCategoryRepository.existsByStoreIdAndNameAndIdNot(storeId, "Bebidas", categoryId))
        .thenReturn(false);
    when(catalogCategoryRepository.saveAndFlush(category)).thenReturn(category);

    var response = updateCatalogCategoryUseCase.execute(categoryId, request);

    assertThat(response.name()).isEqualTo("Bebidas");
    assertThat(response.sortOrder()).isEqualTo(20);
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldThrowWhenCategoryDoesNotExist() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var request = new UpdateCatalogCategoryRequest("Bebidas", 20);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateCatalogCategoryUseCase.execute(categoryId, request))
        .isInstanceOf(CatalogCategoryNotFoundException.class)
        .hasMessageContaining(categoryId.toString());
  }

  @Test
  void shouldRejectDuplicateNameWhenUpdating() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var category = new CatalogCategory(categoryId, store, "Pizzas", 10, true);
    var request = new UpdateCatalogCategoryRequest("Bebidas", 20);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.of(category));
    when(catalogCategoryRepository.existsByStoreIdAndNameAndIdNot(storeId, "Bebidas", categoryId))
        .thenReturn(true);

    assertThatThrownBy(() -> updateCatalogCategoryUseCase.execute(categoryId, request))
        .isInstanceOf(CatalogCategoryAlreadyExistsException.class)
        .hasMessageContaining("Bebidas");
  }

  @Test
  void shouldBlockUpdateWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();
    var request = new UpdateCatalogCategoryRequest("Bebidas", 20);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateCatalogCategoryUseCase.execute(categoryId, request))
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
