package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.UpdateCatalogProductRequest;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateCatalogProductUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final UpdateCatalogProductUseCase updateCatalogProductUseCase =
      new UpdateCatalogProductUseCase(
          storeRepository,
          catalogCategoryRepository,
          catalogProductRepository,
          currentTenantProvider,
          storeOperationalGuard);

  @Test
  void shouldUpdateProduct() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var newCategoryId = UUID.randomUUID();
    var store = store(storeId);
    var category = new CatalogCategory(categoryId, store, "Pizzas", 10, true);
    var newCategory = new CatalogCategory(newCategoryId, store, "Bebidas", 20, true);
    var product =
        new CatalogProduct(
            productId,
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);
    var request =
        new UpdateCatalogProductRequest(
            newCategoryId,
            " Refrigerante ",
            " Lata 350ml ",
            new BigDecimal("7.50"),
            "https://cdn.kfood.local/refrigerante.jpg",
            30,
            false,
            true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogCategoryRepository.findByIdAndStoreId(newCategoryId, storeId))
        .thenReturn(Optional.of(newCategory));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response = updateCatalogProductUseCase.execute(productId, request);

    assertThat(response.categoryId()).isEqualTo(newCategoryId);
    assertThat(response.name()).isEqualTo("Refrigerante");
    assertThat(response.description()).isEqualTo("Lata 350ml");
    assertThat(response.basePrice()).isEqualByComparingTo("7.50");
    assertThat(response.imageUrl()).isEqualTo("https://cdn.kfood.local/refrigerante.jpg");
    assertThat(response.sortOrder()).isEqualTo(30);
    assertThat(response.active()).isFalse();
    assertThat(response.paused()).isTrue();
  }

  @Test
  void shouldUpdateProductActivatingAndResumingIt() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var category = new CatalogCategory(categoryId, store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            productId,
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            "https://cdn.kfood.local/pizza.jpg",
            20,
            false,
            true);
    var request =
        new UpdateCatalogProductRequest(
            categoryId,
            " Pizza Calabresa Especial ",
            " Pizza com calabresa e extra queijo ",
            new BigDecimal("42.90"),
            null,
            25,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.of(category));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response = updateCatalogProductUseCase.execute(productId, request);

    assertThat(response.name()).isEqualTo("Pizza Calabresa Especial");
    assertThat(response.imageUrl()).isNull();
    assertThat(response.active()).isTrue();
    assertThat(response.paused()).isFalse();
  }

  @Test
  void shouldThrowWhenProductDoesNotExist() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var request =
        new UpdateCatalogProductRequest(
            UUID.randomUUID(),
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateCatalogProductUseCase.execute(productId, request))
        .isInstanceOf(CatalogProductNotFoundException.class)
        .hasMessageContaining(productId.toString());
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var request =
        new UpdateCatalogProductRequest(
            UUID.randomUUID(),
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateCatalogProductUseCase.execute(UUID.randomUUID(), request))
        .isInstanceOf(com.kfood.merchant.app.StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldThrowWhenCategoryDoesNotExistInStore() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var store = store(storeId);
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            productId,
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);
    var request =
        new UpdateCatalogProductRequest(
            categoryId,
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogCategoryRepository.findByIdAndStoreId(categoryId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateCatalogProductUseCase.execute(productId, request))
        .isInstanceOf(CatalogCategoryNotFoundException.class)
        .hasMessageContaining(categoryId.toString());
  }

  @Test
  void shouldBlockUpdateWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();
    var request =
        new UpdateCatalogProductRequest(
            UUID.randomUUID(),
            "Pizza Calabresa",
            "Pizza com calabresa",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> updateCatalogProductUseCase.execute(productId, request))
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
