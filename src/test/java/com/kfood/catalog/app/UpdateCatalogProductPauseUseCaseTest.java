package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.UpdateCatalogProductPauseRequest;
import com.kfood.catalog.app.audit.CatalogProductAuditPort;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateCatalogProductPauseUseCaseTest {

  private final StoreRepository storeRepository = org.mockito.Mockito.mock(StoreRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      org.mockito.Mockito.mock(CatalogProductRepository.class);
  private final CurrentTenantProvider currentTenantProvider =
      org.mockito.Mockito.mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      org.mockito.Mockito.mock(CurrentAuthenticatedUserProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final CatalogProductAuditPort catalogProductAuditPort =
      org.mockito.Mockito.mock(CatalogProductAuditPort.class);
  private final UpdateCatalogProductPauseUseCase updateCatalogProductPauseUseCase =
      new UpdateCatalogProductPauseUseCase(
          storeRepository,
          catalogProductRepository,
          currentTenantProvider,
          currentAuthenticatedUserProvider,
          storeOperationalGuard,
          catalogProductAuditPort);

  @Test
  void shouldPauseProductChangingFlag() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId, true, false);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response =
        updateCatalogProductPauseUseCase.execute(
            productId, new UpdateCatalogProductPauseRequest(true, "Ingredient unavailable"));

    verify(product).pause();
    verify(catalogProductRepository).saveAndFlush(product);
    verify(catalogProductAuditPort)
        .recordProductPauseChanged(storeId, productId, true, "Ingredient unavailable", userId);
    assertThat(response.id()).isEqualTo(productId);
    assertThat(response.paused()).isTrue();
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldUnpauseProductRestoringAvailability() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId, true, true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response =
        updateCatalogProductPauseUseCase.execute(
            productId, new UpdateCatalogProductPauseRequest(false, "Back in stock"));

    verify(product).resume();
    verify(catalogProductRepository).saveAndFlush(product);
    verify(catalogProductAuditPort)
        .recordProductPauseChanged(storeId, productId, false, "Back in stock", userId);
    assertThat(response.paused()).isFalse();
    assertThat(response.active()).isTrue();
  }

  @Test
  void shouldRejectWhenProductDoesNotBelongToTenant() {
    var storeId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(userId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                updateCatalogProductPauseUseCase.execute(
                    productId, new UpdateCatalogProductPauseRequest(true, "Unavailable")))
        .isInstanceOf(CatalogProductNotFoundException.class)
        .hasMessageContaining(productId.toString());

    verify(catalogProductRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    verifyNoInteractions(catalogProductAuditPort);
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(UUID.randomUUID());
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                updateCatalogProductPauseUseCase.execute(
                    productId, new UpdateCatalogProductPauseRequest(true, "Unavailable")))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldBlockPauseWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(UUID.randomUUID());
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                updateCatalogProductPauseUseCase.execute(
                    productId, new UpdateCatalogProductPauseRequest(true, "Unavailable")))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SUSPENDED");

    verifyNoInteractions(catalogProductAuditPort);
  }

  private CatalogProduct product(Store store, UUID productId, boolean active, boolean paused) {
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    return spy(
        new CatalogProduct(
            productId,
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            active,
            paused));
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
