package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.catalog.api.CreateCatalogOptionGroupRequest;
import com.kfood.catalog.api.CreateCatalogOptionItemRequest;
import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionGroupRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotActiveException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateCatalogOptionGroupUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final CatalogOptionGroupRepository catalogOptionGroupRepository =
      mock(CatalogOptionGroupRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final StoreOperationalGuard storeOperationalGuard = new StoreOperationalGuard();
  private final CreateCatalogOptionGroupUseCase createCatalogOptionGroupUseCase =
      new CreateCatalogOptionGroupUseCase(
          storeRepository,
          catalogProductRepository,
          catalogOptionGroupRepository,
          currentTenantProvider,
          storeOperationalGuard);

  @Test
  void shouldCreateOptionGroup() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    var request =
        new CreateCatalogOptionGroupRequest(
            " Stuffed Crust ",
            null,
            null,
            null,
            null,
            List.of(
                new CreateCatalogOptionItemRequest("Catupiry", new BigDecimal("8.00"), true, 10),
                new CreateCatalogOptionItemRequest("Cheddar", new BigDecimal("7.50"), null, null)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogOptionGroupRepository.saveAndFlush(any(CatalogOptionGroup.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = createCatalogOptionGroupUseCase.execute(productId, request);

    assertThat(response.productId()).isEqualTo(productId);
    assertThat(response.name()).isEqualTo("Stuffed Crust");
    assertThat(response.minSelect()).isZero();
    assertThat(response.maxSelect()).isEqualTo(1);
    assertThat(response.required()).isFalse();
    assertThat(response.active()).isTrue();
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).name()).isEqualTo("Catupiry");
    assertThat(response.items().get(0).extraPrice()).isEqualByComparingTo("8.00");
    assertThat(response.items().get(0).sortOrder()).isEqualTo(10);
    assertThat(response.items().get(1).name()).isEqualTo("Cheddar");
    assertThat(response.items().get(1).sortOrder()).isZero();
  }

  @Test
  void shouldRejectWhenProductDoesNotExistForTenant() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var request = new CreateCatalogOptionGroupRequest("Sauces", 0, 2, false, true, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> createCatalogOptionGroupUseCase.execute(productId, request))
        .isInstanceOf(CatalogProductNotFoundException.class)
        .hasMessageContaining(productId.toString());

    verify(catalogOptionGroupRepository, never()).saveAndFlush(any(CatalogOptionGroup.class));
  }

  @Test
  void shouldRejectWhenMaxSelectIsLessThanMinSelect() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    var request = new CreateCatalogOptionGroupRequest("Sauces", 2, 1, false, true, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));

    assertThatThrownBy(() -> createCatalogOptionGroupUseCase.execute(productId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("maxSelect must be greater than or equal to minSelect");

    verify(catalogOptionGroupRepository, never()).saveAndFlush(any(CatalogOptionGroup.class));
  }

  @Test
  void shouldThrowWhenStoreDoesNotExist() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var request = new CreateCatalogOptionGroupRequest("Sauces", 0, 1, false, true, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createCatalogOptionGroupUseCase.execute(productId, request))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(storeId.toString());
  }

  @Test
  void shouldBlockCreateWhenStoreIsSuspended() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    store.activate();
    store.suspend();
    var request = new CreateCatalogOptionGroupRequest("Sauces", 0, 1, false, true, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> createCatalogOptionGroupUseCase.execute(productId, request))
        .isInstanceOf(StoreNotActiveException.class)
        .hasMessageContaining("SUSPENDED");
  }

  @Test
  void shouldRejectNegativeExtraPrice() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    var request =
        new CreateCatalogOptionGroupRequest(
            "Stuffed Crust",
            0,
            1,
            false,
            true,
            List.of(
                new CreateCatalogOptionItemRequest("Catupiry", new BigDecimal("-1.00"), true, 10)));

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));

    assertThatThrownBy(() -> createCatalogOptionGroupUseCase.execute(productId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessage("extraPrice must be greater than or equal to zero");

    verify(catalogOptionGroupRepository, never()).saveAndFlush(any(CatalogOptionGroup.class));
  }

  @Test
  void shouldCreateInactiveGroupWithoutItemsAndInactiveItem() {
    var storeId = UUID.randomUUID();
    var productId = UUID.randomUUID();
    var store = store(storeId);
    var product = product(store, productId);
    var requestWithoutItems =
        new CreateCatalogOptionGroupRequest("Sauces", 1, 2, true, false, null);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(catalogProductRepository.findByIdAndStoreId(productId, storeId))
        .thenReturn(Optional.of(product));
    when(catalogOptionGroupRepository.saveAndFlush(any(CatalogOptionGroup.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var emptyResponse = createCatalogOptionGroupUseCase.execute(productId, requestWithoutItems);

    assertThat(emptyResponse.active()).isFalse();
    assertThat(emptyResponse.items()).isEmpty();

    var requestWithInactiveItem =
        new CreateCatalogOptionGroupRequest(
            "Sauces",
            0,
            1,
            false,
            true,
            List.of(new CreateCatalogOptionItemRequest("Barbecue", BigDecimal.ZERO, false, null)));

    var responseWithItem =
        createCatalogOptionGroupUseCase.execute(productId, requestWithInactiveItem);

    assertThat(responseWithItem.items())
        .singleElement()
        .satisfies(item -> assertThat(item.active()).isFalse());
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
