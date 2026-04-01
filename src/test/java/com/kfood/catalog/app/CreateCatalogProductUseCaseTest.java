package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.CreateCatalogProductRequest;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateCatalogProductUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final CreateCatalogProductUseCase createCatalogProductUseCase =
      new CreateCatalogProductUseCase(
          storeRepository,
          catalogCategoryRepository,
          catalogProductRepository,
          currentTenantProvider,
          storeOperationalGuard);

  @Test
  void shouldCreateProduct() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var category = new CatalogCategory(categoryId, store, "Pizzas", 10, true);
    var request =
        new CreateCatalogProductRequest(
            categoryId,
            " Pizza Calabresa ",
            " Pizza com calabresa e cebola ",
            new BigDecimal("39.90"),
            "https://cdn.kfood.local/pizza.jpg",
            20,
            true,
            false);
    var savedProduct =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            "https://cdn.kfood.local/pizza.jpg",
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.of(category));
    when(catalogProductRepository.saveAndFlush(any(CatalogProduct.class))).thenReturn(savedProduct);

    var response = createCatalogProductUseCase.execute(request);

    assertThat(response.id()).isEqualTo(savedProduct.getId());
    assertThat(response.categoryId()).isEqualTo(categoryId);
    assertThat(response.name()).isEqualTo("Pizza Calabresa");
    assertThat(response.basePrice()).isEqualByComparingTo("39.90");
    assertThat(response.active()).isTrue();
    assertThat(response.paused()).isFalse();
  }

  @Test
  void shouldRejectWhenCategoryDoesNotExistInStore() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var request =
        new CreateCatalogProductRequest(
            categoryId,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> createCatalogProductUseCase.execute(request))
        .isInstanceOf(CatalogCategoryNotFoundException.class)
        .hasMessageContaining(categoryId.toString());
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var request =
        new CreateCatalogProductRequest(
            UUID.randomUUID(),
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createCatalogProductUseCase.execute(request))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldBlockCreateWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();
    var request =
        new CreateCatalogProductRequest(
            categoryId,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> createCatalogProductUseCase.execute(request))
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
