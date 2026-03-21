package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListCatalogProductsUseCaseTest {

  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final ListCatalogProductsUseCase listCatalogProductsUseCase =
      new ListCatalogProductsUseCase(catalogProductRepository, currentTenantProvider);

  @Test
  void shouldListProductsForStore() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(catalogProductRepository.findAllByStoreIdOrderBySortOrderAscNameAsc(storeId))
        .thenReturn(
            List.of(
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "Pizza Mussarela",
                    "Pizza com mussarela",
                    new BigDecimal("34.90"),
                    null,
                    10,
                    true,
                    false),
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "Pizza Calabresa",
                    "Pizza com calabresa",
                    new BigDecimal("39.90"),
                    null,
                    20,
                    false,
                    true)));

    var response = listCatalogProductsUseCase.execute();

    assertThat(response).hasSize(2);
    assertThat(response.getFirst().name()).isEqualTo("Pizza Mussarela");
    assertThat(response.getLast().name()).isEqualTo("Pizza Calabresa");
  }
}
