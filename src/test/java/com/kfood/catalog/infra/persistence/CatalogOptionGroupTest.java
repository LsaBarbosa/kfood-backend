package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogOptionGroupTest {

  @Test
  void shouldCreateValidOptionGroup() {
    var product = product(store());
    var optionGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product, " Stuffed Crust ", 0, 1, false, true);

    assertThat(optionGroup.getId()).isNotNull();
    assertThat(optionGroup.getProduct()).isSameAs(product);
    assertThat(optionGroup.getName()).isEqualTo("Stuffed Crust");
    assertThat(optionGroup.getMinSelect()).isZero();
    assertThat(optionGroup.getMaxSelect()).isEqualTo(1);
    assertThat(optionGroup.isRequired()).isFalse();
    assertThat(optionGroup.isActive()).isTrue();
  }

  @Test
  void shouldChangeOptionGroupState() {
    var optionGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product(store()), "Sauces", 0, 2, false, true);

    optionGroup.changeName(" Premium Sauces ");
    optionGroup.changeSelectionRange(1, 3);
    optionGroup.deactivate();

    assertThat(optionGroup.getName()).isEqualTo("Premium Sauces");
    assertThat(optionGroup.getMinSelect()).isEqualTo(1);
    assertThat(optionGroup.getMaxSelect()).isEqualTo(3);
    assertThat(optionGroup.isActive()).isFalse();

    optionGroup.activate();

    assertThat(optionGroup.isActive()).isTrue();
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new CatalogOptionGroup(
                    UUID.randomUUID(), product(store()), "   ", 0, 1, false, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must not be blank");
  }

  @Test
  void shouldRejectNegativeMinSelect() {
    assertThatThrownBy(
            () ->
                new CatalogOptionGroup(
                    UUID.randomUUID(), product(store()), "Sauces", -1, 1, false, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minSelect must be greater than or equal to zero");
  }

  @Test
  void shouldRejectMaxSelectLowerThanMinSelect() {
    assertThatThrownBy(
            () ->
                new CatalogOptionGroup(
                    UUID.randomUUID(), product(store()), "Sauces", 2, 1, false, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maxSelect must be greater than or equal to minSelect");
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var optionGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product(store()), "Sauces", 0, 2, false, true);
    Method prePersist = CatalogOptionGroup.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);

    prePersist.invoke(optionGroup);

    assertThat(optionGroup.getMaxSelect()).isEqualTo(2);
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<CatalogOptionGroup> constructor = CatalogOptionGroup.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private CatalogProduct product(Store store) {
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    return new CatalogProduct(
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
  }

  private Store store() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }
}
