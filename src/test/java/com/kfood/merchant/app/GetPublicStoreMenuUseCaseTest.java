package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPublicStoreMenuUseCaseTest {

  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final CatalogCategoryRepository catalogCategoryRepository =
      mock(CatalogCategoryRepository.class);
  private final CatalogProductRepository catalogProductRepository =
      mock(CatalogProductRepository.class);
  private final GetPublicStoreMenuUseCase getPublicStoreMenuUseCase =
      new GetPublicStoreMenuUseCase(
          storeRepository, catalogCategoryRepository, catalogProductRepository);

  @Test
  void shouldReturnOnlyVisibleOrderedMenuForStore() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var anotherStore = store("outra-loja", "54.550.752/0001-55");
    var drinks = new CatalogCategory(UUID.randomUUID(), store, "Bebidas", 10, true);
    var pizzas = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true);
    var hidden = new CatalogCategory(UUID.randomUUID(), store, "Oculta", 30, false);
    var foreignCategory =
        new CatalogCategory(UUID.randomUUID(), anotherStore, "Estrangeira", 5, true);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(catalogCategoryRepository.findAllByStoreIdAndActiveTrueOrderBySortOrderAscNameAsc(
            store.getId()))
        .thenReturn(List.of(drinks, pizzas, foreignCategory));
    when(catalogProductRepository
            .findAllByStoreIdAndActiveTrueAndPausedFalseOrderBySortOrderAscNameAsc(store.getId()))
        .thenReturn(
            List.of(
                product(store, drinks, "Refrigerante", "Lata 350ml", "7.50", 10, true, false),
                product(
                    store,
                    pizzas,
                    "Pizza Calabresa",
                    "Pizza com calabresa",
                    "39.90",
                    20,
                    true,
                    false),
                product(store, hidden, "Produto Oculto", "Nao deve sair", "10.00", 30, true, false),
                product(
                    anotherStore,
                    foreignCategory,
                    "Produto Estrangeiro",
                    "Nao deve sair",
                    "11.00",
                    40,
                    true,
                    false),
                product(store, pizzas, "Pizza Pausada", "Nao deve sair", "42.00", 50, true, true)));

    var response = getPublicStoreMenuUseCase.execute(" loja-do-bairro ");

    assertThat(response.categories()).hasSize(2);
    assertThat(response.categories().getFirst().name()).isEqualTo("Bebidas");
    assertThat(response.categories().getLast().name()).isEqualTo("Pizzas");
    assertThat(response.categories().getFirst().products())
        .extracting(item -> item.name())
        .containsExactly("Refrigerante");
    assertThat(response.categories().getLast().products())
        .extracting(item -> item.name())
        .containsExactly("Pizza Calabresa");
  }

  @Test
  void shouldReturnNotFoundWhenSlugDoesNotExist() {
    when(storeRepository.findBySlug("nao-existe")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getPublicStoreMenuUseCase.execute("nao-existe"))
        .isInstanceOf(StoreSlugNotFoundException.class)
        .hasMessageContaining("nao-existe");
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }

  private CatalogProduct product(
      Store store,
      CatalogCategory category,
      String name,
      String description,
      String basePrice,
      int sortOrder,
      boolean active,
      boolean paused) {
    return new CatalogProduct(
        UUID.randomUUID(),
        store,
        category,
        name,
        description,
        new BigDecimal(basePrice),
        null,
        sortOrder,
        active,
        paused);
  }
}
