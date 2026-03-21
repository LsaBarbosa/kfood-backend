package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogCategoryTest {

  @Test
  void shouldCreateValidCategory() {
    var store = store("loja-do-bairro", "45.723.174/0001-10");
    var category = new CatalogCategory(UUID.randomUUID(), store, " Pizzas tradicionais ", 10, true);

    assertThat(category.getId()).isNotNull();
    assertThat(category.getStore()).isSameAs(store);
    assertThat(category.getName()).isEqualTo("Pizzas tradicionais");
    assertThat(category.getSortOrder()).isEqualTo(10);
    assertThat(category.isActive()).isTrue();
  }

  @Test
  void shouldChangeCategoryState() {
    var category =
        new CatalogCategory(
            UUID.randomUUID(), store("loja-do-bairro", "45.723.174/0001-10"), "Pizzas", 10, true);

    category.changeName(" Bebidas ");
    category.changeSortOrder(20);
    category.deactivate();

    assertThat(category.getName()).isEqualTo("Bebidas");
    assertThat(category.getSortOrder()).isEqualTo(20);
    assertThat(category.isActive()).isFalse();

    category.activate();

    assertThat(category.isActive()).isTrue();
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new CatalogCategory(
                    UUID.randomUUID(),
                    store("loja-do-bairro", "45.723.174/0001-10"),
                    "   ",
                    10,
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must not be blank");
  }

  @Test
  void shouldRejectNegativeSortOrder() {
    assertThatThrownBy(
            () ->
                new CatalogCategory(
                    UUID.randomUUID(),
                    store("loja-do-bairro", "45.723.174/0001-10"),
                    "Pizzas",
                    -1,
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sortOrder must be greater than or equal to zero");
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var category =
        new CatalogCategory(
            UUID.randomUUID(), store("loja-do-bairro", "45.723.174/0001-10"), "Pizzas", 10, true);
    Method prePersist = CatalogCategory.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);

    prePersist.invoke(category);

    assertThat(category.getName()).isEqualTo("Pizzas");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<CatalogCategory> constructor = CatalogCategory.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private Store store(String slug, String cnpj) {
    return new Store(
        UUID.randomUUID(), "Loja do Bairro", slug, cnpj, "21999990000", "America/Sao_Paulo");
  }
}
