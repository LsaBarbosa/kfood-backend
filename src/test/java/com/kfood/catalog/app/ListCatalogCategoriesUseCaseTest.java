package com.kfood.catalog.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListCatalogCategoriesUseCaseTest {

  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final ListCatalogCategoriesUseCase listCatalogCategoriesUseCase =
      new ListCatalogCategoriesUseCase(catalogCategoryRepository, currentTenantProvider);

  @Test
  void shouldListCategoriesForStore() {
    var storeId = UUID.randomUUID();
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(catalogCategoryRepository.findAllByStoreIdOrderBySortOrderAscNameAsc(storeId))
        .thenReturn(
            List.of(
                new CatalogCategory(UUID.randomUUID(), store, "Bebidas", 5, true),
                new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, false)));

    var response = listCatalogCategoriesUseCase.execute();

    assertThat(response).hasSize(2);
    assertThat(response.getFirst().name()).isEqualTo("Bebidas");
    assertThat(response.getLast().name()).isEqualTo("Pizzas");
  }
}
