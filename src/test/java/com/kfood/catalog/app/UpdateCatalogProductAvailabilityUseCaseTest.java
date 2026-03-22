package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.CatalogProductAvailabilityWindowRequest;
import com.kfood.catalog.api.UpdateCatalogProductAvailabilityRequest;
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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateCatalogProductAvailabilityUseCaseTest {

  private final StoreRepository storeRepository = org.mockito.Mockito.mock(StoreRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      org.mockito.Mockito.mock(CatalogProductRepository.class);
  private final CurrentTenantProvider currentTenantProvider =
      org.mockito.Mockito.mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final UpdateCatalogProductAvailabilityUseCase updateCatalogProductAvailabilityUseCase =
      new UpdateCatalogProductAvailabilityUseCase(
          storeRepository, catalogProductRepository, currentTenantProvider, storeOperationalGuard);

  @Test
  void shouldReplaceAvailabilityWindows() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    var request =
        new UpdateCatalogProductAvailabilityRequest(
            List.of(
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0), true),
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0), true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response = updateCatalogProductAvailabilityUseCase.execute(productId, request);

    verify(catalogProductRepository).saveAndFlush(product);
    assertThat(response.productId()).isEqualTo(productId);
    assertThat(response.windows()).hasSize(2);
    assertThat(response.windows())
        .extracting(window -> window.dayOfWeek())
        .containsOnly(DayOfWeek.MONDAY);
  }

  @Test
  void shouldAllowClearingAvailabilityWindows() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    product.replaceAvailabilityWindows(
        List.of(
            new com.kfood.catalog.infra.persistence.CatalogProductAvailabilityWindow(
                UUID.randomUUID(),
                product,
                DayOfWeek.MONDAY,
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                true)));
    var request = new UpdateCatalogProductAvailabilityRequest(List.of());

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response = updateCatalogProductAvailabilityUseCase.execute(productId, request);

    assertThat(response.windows()).isEmpty();
    assertThat(product.getAvailabilityWindows()).isEmpty();
  }

  @Test
  void shouldIgnoreInactiveWindowsWhenValidatingOverlaps() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    var request =
        new UpdateCatalogProductAvailabilityRequest(
            List.of(
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0), false),
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0), true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogProductRepository.saveAndFlush(product)).thenReturn(product);

    var response = updateCatalogProductAvailabilityUseCase.execute(productId, request);

    assertThat(response.windows()).hasSize(2);
  }

  @Test
  void shouldRejectInvalidTimeRange() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var request =
        new UpdateCatalogProductAvailabilityRequest(
            List.of(
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(12, 0), true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product(store, productId)));

    assertThatThrownBy(() -> updateCatalogProductAvailabilityUseCase.execute(productId, request))
        .isInstanceOf(InvalidCatalogProductAvailabilityException.class)
        .hasMessageContaining("startTime must be before endTime");

    verify(catalogProductRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldRejectInvalidTimeRangeEvenForInactiveWindow() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var request =
        new UpdateCatalogProductAvailabilityRequest(
            List.of(
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(12, 0), false)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product(store, productId)));

    assertThatThrownBy(() -> updateCatalogProductAvailabilityUseCase.execute(productId, request))
        .isInstanceOf(InvalidCatalogProductAvailabilityException.class)
        .hasMessageContaining("startTime must be before endTime");
  }

  @Test
  void shouldRejectOverlappingWindowsOnSameDay() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var request =
        new UpdateCatalogProductAvailabilityRequest(
            List.of(
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0), true),
                new CatalogProductAvailabilityWindowRequest(
                    DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(16, 0), true)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product(store, productId)));

    assertThatThrownBy(() -> updateCatalogProductAvailabilityUseCase.execute(productId, request))
        .isInstanceOf(InvalidCatalogProductAvailabilityException.class)
        .hasMessageContaining("Overlapping availability windows");

    verify(catalogProductRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                updateCatalogProductAvailabilityUseCase.execute(
                    productId, new UpdateCatalogProductAvailabilityRequest(List.of())))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldBlockUpdateWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () ->
                updateCatalogProductAvailabilityUseCase.execute(
                    productId, new UpdateCatalogProductAvailabilityRequest(List.of())))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SUSPENDED");
  }

  @Test
  void shouldThrowWhenProductDoesNotExist() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findDetailedByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                updateCatalogProductAvailabilityUseCase.execute(
                    productId, new UpdateCatalogProductAvailabilityRequest(List.of())))
        .isInstanceOf(CatalogProductNotFoundException.class)
        .hasMessageContaining(productId.toString());
  }

  private CatalogProduct product(Store store, UUID productId) {
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    return new CatalogProduct(
        productId,
        store,
        category,
        "Pizza Calabresa",
        "Pizza com calabresa e cebola",
        new BigDecimal("39.90"),
        null,
        20,
        true,
        false);
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
