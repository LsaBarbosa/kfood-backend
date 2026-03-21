package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogProductTest {

  @Test
  void shouldCreateValidProduct() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            " Pizza Calabresa ",
            " Pizza com calabresa e cebola ",
            new BigDecimal("39.9"),
            " https://cdn.kfood.local/pizza.jpg ",
            20,
            true,
            false);

    assertThat(product.getId()).isNotNull();
    assertThat(product.getStore()).isSameAs(store);
    assertThat(product.getCategory()).isSameAs(category);
    assertThat(product.getName()).isEqualTo("Pizza Calabresa");
    assertThat(product.getDescription()).isEqualTo("Pizza com calabresa e cebola");
    assertThat(product.getBasePrice()).isEqualByComparingTo("39.90");
    assertThat(product.getImageUrl()).isEqualTo("https://cdn.kfood.local/pizza.jpg");
    assertThat(product.getSortOrder()).isEqualTo(20);
    assertThat(product.isActive()).isTrue();
    assertThat(product.isPaused()).isFalse();
  }

  @Test
  void shouldChangeProductState() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);

    product.pause();
    product.deactivate();

    assertThat(product.isPaused()).isTrue();
    assertThat(product.isActive()).isFalse();

    product.resume();
    product.activate();

    assertThat(product.isPaused()).isFalse();
    assertThat(product.isActive()).isTrue();
  }

  @Test
  void shouldRejectBlankDescription() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    assertThatThrownBy(
            () ->
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "Pizza Calabresa",
                    "   ",
                    new BigDecimal("39.90"),
                    null,
                    20,
                    true,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("description must not be blank");
  }

  @Test
  void shouldRejectBlankName() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    assertThatThrownBy(
            () ->
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "   ",
                    "Pizza com calabresa e cebola",
                    new BigDecimal("39.90"),
                    null,
                    20,
                    true,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must not be blank");
  }

  @Test
  void shouldRejectNegativePrice() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    assertThatThrownBy(
            () ->
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "Pizza Calabresa",
                    "Pizza com calabresa e cebola",
                    new BigDecimal("-1.00"),
                    null,
                    20,
                    true,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("basePrice must be greater than or equal to zero");
  }

  @Test
  void shouldRejectNegativeSortOrder() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    assertThatThrownBy(
            () ->
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "Pizza Calabresa",
                    "Pizza com calabresa e cebola",
                    new BigDecimal("39.90"),
                    null,
                    -1,
                    true,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sortOrder must be greater than or equal to zero");
  }

  @Test
  void shouldRejectCategoryFromAnotherStore() {
    var store = store("loja-a", "45.723.174/0001-10");
    var anotherStore = store("loja-b", "54.550.752/0001-55");
    var category = new CatalogCategory(UUID.randomUUID(), anotherStore, "Pizzas", 10, true);

    assertThatThrownBy(
            () ->
                new CatalogProduct(
                    UUID.randomUUID(),
                    store,
                    category,
                    "Pizza Calabresa",
                    "Pizza com calabresa e cebola",
                    new BigDecimal("39.90"),
                    null,
                    20,
                    true,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("product store must match category store");
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
            UUID.randomUUID(),
            store,
            category,
            "Pizza Calabresa",
            "Pizza com calabresa e cebola",
            new BigDecimal("39.90"),
            null,
            20,
            true,
            false);
    Method prePersist = CatalogProduct.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);

    prePersist.invoke(product);

    assertThat(product.getBasePrice()).isEqualByComparingTo("39.90");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<CatalogProduct> constructor = CatalogProduct.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
