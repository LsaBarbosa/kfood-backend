package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
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

class DeactivateCatalogProductUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final DeactivateCatalogProductUseCase deactivateCatalogProductUseCase =
      new DeactivateCatalogProductUseCase(
          storeRepository, catalogProductRepository, currentTenantProvider, storeOperationalGuard);

  @Test
  void shouldDeactivateProduct() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
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

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response = deactivateCatalogProductUseCase.execute(productId);

    assertThat(response.active()).isFalse();
    assertThat(response.name()).isEqualTo("Pizza Calabresa");
  }

  @Test
  void shouldThrowWhenProductDoesNotExist() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store(storeId)));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> deactivateCatalogProductUseCase.execute(productId))
        .isInstanceOf(CatalogProductNotFoundException.class)
        .hasMessageContaining(productId.toString());
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> deactivateCatalogProductUseCase.execute(UUID.randomUUID()))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldBlockDeactivateWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> deactivateCatalogProductUseCase.execute(productId))
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
