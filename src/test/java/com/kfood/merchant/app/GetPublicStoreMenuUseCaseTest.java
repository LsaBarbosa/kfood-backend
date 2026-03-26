package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogOptionGroup;
import com.kfood.catalog.infra.persistence.CatalogOptionItem;
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

  @SuppressWarnings("unused")
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
    var drinks = new CatalogCategory(UUID.randomUUID(), store, "Bebidas", 10, true);
    var pizzas = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true);

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(
            org.mockito.ArgumentMatchers.eq(store.getId()),
            any(java.time.DayOfWeek.class),
            any(java.time.LocalTime.class)))
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
                    false)));

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
    assertThat(response.categories().getLast().products().getFirst().optionGroups()).isEmpty();
  }

  @Test
  void shouldReturnEmptyCategoriesWhenStoreHasNoVisibleProducts() {
    var store = store("loja-vazia", "45.723.174/0001-10");

    when(storeRepository.findBySlug("loja-vazia")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(
            org.mockito.ArgumentMatchers.eq(store.getId()),
            any(java.time.DayOfWeek.class),
            any(java.time.LocalTime.class)))
        .thenReturn(List.of());

    var response = getPublicStoreMenuUseCase.execute("loja-vazia");

    assertThat(response.categories()).isEmpty();
  }

  @Test
  void shouldHideProductsOutsideAvailabilityWindow() {
    var store = store("loja-com-janela", "45.723.174/0001-10");
    var pizzas = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true);
    var visibleProduct =
        product(store, pizzas, "Pizza Almoco", "Disponivel no almoco", "39.90", 10, true, false);

    when(storeRepository.findBySlug("loja-com-janela")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(
            org.mockito.ArgumentMatchers.eq(store.getId()),
            any(java.time.DayOfWeek.class),
            any(java.time.LocalTime.class)))
        .thenReturn(List.of(visibleProduct));

    var response = getPublicStoreMenuUseCase.execute("loja-com-janela");

    assertThat(response.categories()).hasSize(1);
    assertThat(response.categories().getFirst().products())
        .extracting(item -> item.name())
        .containsExactly("Pizza Almoco");
  }

  @Test
  void shouldReturnProductWithSingleGroupAndMultipleOptions() {
    var store = store("loja-com-opcoes", "45.723.174/0001-10");
    var pizzas = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true);
    var product =
        product(store, pizzas, "Pizza Calabresa", "Pizza com calabresa", "39.90", 10, true, false);
    var group = optionGroup(product, "Bordas", 1, 2, true, true);
    group.addItem(optionItem(group, "Catupiry", "8.00", true, 10));
    group.addItem(optionItem(group, "Cheddar", "7.50", true, 20));

    when(storeRepository.findBySlug("loja-com-opcoes")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(
            org.mockito.ArgumentMatchers.eq(store.getId()),
            any(java.time.DayOfWeek.class),
            any(java.time.LocalTime.class)))
        .thenReturn(List.of(product));

    var response = getPublicStoreMenuUseCase.execute("loja-com-opcoes");

    var optionGroups = response.categories().getFirst().products().getFirst().optionGroups();
    assertThat(optionGroups).hasSize(1);
    assertThat(optionGroups.getFirst().name()).isEqualTo("Bordas");
    assertThat(optionGroups.getFirst().required()).isTrue();
    assertThat(optionGroups.getFirst().items()).extracting(item -> item.name())
        .containsExactly("Catupiry", "Cheddar");
  }

  @Test
  void shouldReturnProductWithMultipleGroups() {
    var store = store("loja-com-multiplos-grupos", "45.723.174/0001-10");
    var pizzas = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true);
    var product =
        product(store, pizzas, "Pizza Especial", "Pizza especial", "45.90", 10, true, false);
    var crust = optionGroup(product, "Bordas", 0, 1, false, true);
    crust.addItem(optionItem(crust, "Catupiry", "8.00", true, 10));
    var sauce = optionGroup(product, "Molhos", 0, 2, false, true);
    sauce.addItem(optionItem(sauce, "Barbecue", "2.50", true, 5));

    when(storeRepository.findBySlug("loja-com-multiplos-grupos")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(
            org.mockito.ArgumentMatchers.eq(store.getId()),
            any(java.time.DayOfWeek.class),
            any(java.time.LocalTime.class)))
        .thenReturn(List.of(product));

    var response = getPublicStoreMenuUseCase.execute("loja-com-multiplos-grupos");

    assertThat(response.categories().getFirst().products().getFirst().optionGroups())
        .extracting(group -> group.name())
        .containsExactly("Bordas", "Molhos");
  }

  @Test
  void shouldFilterInactiveGroupsAndItems() {
    var store = store("loja-com-filtro", "45.723.174/0001-10");
    var pizzas = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 20, true);
    var product =
        product(store, pizzas, "Pizza Frango", "Pizza de frango", "41.90", 10, true, false);
    var activeGroup = optionGroup(product, "Molhos", 0, 2, false, true);
    activeGroup.addItem(optionItem(activeGroup, "Barbecue", "2.50", true, 5));
    activeGroup.addItem(optionItem(activeGroup, "Alho", "1.50", false, 10));
    var inactiveGroup = optionGroup(product, "Bebidas", 0, 1, false, false);
    inactiveGroup.addItem(optionItem(inactiveGroup, "Coca-Cola", "7.00", true, 1));

    when(storeRepository.findBySlug("loja-com-filtro")).thenReturn(Optional.of(store));
    when(catalogProductRepository.findAllVisibleForPublicMenu(
            org.mockito.ArgumentMatchers.eq(store.getId()),
            any(java.time.DayOfWeek.class),
            any(java.time.LocalTime.class)))
        .thenReturn(List.of(product));

    var response = getPublicStoreMenuUseCase.execute("loja-com-filtro");

    var optionGroups = response.categories().getFirst().products().getFirst().optionGroups();
    assertThat(optionGroups).hasSize(1);
    assertThat(optionGroups.getFirst().name()).isEqualTo("Molhos");
    assertThat(optionGroups.getFirst().items()).extracting(item -> item.name())
        .containsExactly("Barbecue");
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

  private CatalogOptionGroup optionGroup(
      CatalogProduct product, String name, int minSelect, int maxSelect, boolean required, boolean active) {
    var group =
        new CatalogOptionGroup(
            UUID.randomUUID(), product, name, minSelect, maxSelect, required, active);
    attachOptionGroup(product, group);
    return group;
  }

  private CatalogOptionItem optionItem(
      CatalogOptionGroup group, String name, String extraPrice, boolean active, int sortOrder) {
    return new CatalogOptionItem(
        UUID.randomUUID(), group, name, new BigDecimal(extraPrice), active, sortOrder);
  }

  private void attachOptionGroup(CatalogProduct product, CatalogOptionGroup group) {
    try {
      var field = CatalogProduct.class.getDeclaredField("optionGroups");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      var groups = (List<CatalogOptionGroup>) field.get(product);
      groups.add(group);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
