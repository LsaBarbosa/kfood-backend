package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogOptionItemTest {

  @Test
  void shouldCreateValidOptionItem() {
    var optionGroup = optionGroup();
    var optionItem =
        new CatalogOptionItem(
            UUID.randomUUID(), optionGroup, " Catupiry ", new BigDecimal("8.0"), true, 10);

    assertThat(optionItem.getId()).isNotNull();
    assertThat(optionItem.getOptionGroup()).isSameAs(optionGroup);
    assertThat(optionItem.getName()).isEqualTo("Catupiry");
    assertThat(optionItem.getExtraPrice()).isEqualByComparingTo("8.00");
    assertThat(optionItem.isActive()).isTrue();
    assertThat(optionItem.getSortOrder()).isEqualTo(10);
  }

  @Test
  void shouldChangeOptionItemState() {
    var optionItem =
        new CatalogOptionItem(
            UUID.randomUUID(), optionGroup(), "Catupiry", new BigDecimal("8.00"), true, 10);

    optionItem.deactivate();
    assertThat(optionItem.isActive()).isFalse();

    optionItem.activate();
    assertThat(optionItem.isActive()).isTrue();
  }

  @Test
  void shouldRejectNegativeExtraPrice() {
    assertThatThrownBy(
            () ->
                new CatalogOptionItem(
                    UUID.randomUUID(),
                    optionGroup(),
                    "Catupiry",
                    new BigDecimal("-1.00"),
                    true,
                    10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraPrice must be greater than or equal to zero");
  }

  @Test
  void shouldRejectNegativeSortOrder() {
    assertThatThrownBy(
            () ->
                new CatalogOptionItem(
                    UUID.randomUUID(), optionGroup(), "Catupiry", new BigDecimal("8.00"), true, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sortOrder must be greater than or equal to zero");
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var optionItem =
        new CatalogOptionItem(
            UUID.randomUUID(), optionGroup(), "Catupiry", new BigDecimal("8.00"), true, 10);
    Method prePersist = CatalogOptionItem.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);

    prePersist.invoke(optionItem);

    assertThat(optionItem.getExtraPrice()).isEqualByComparingTo("8.00");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<CatalogOptionItem> constructor = CatalogOptionItem.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new CatalogOptionItem(
                    UUID.randomUUID(), optionGroup(), " ", new BigDecimal("8.00"), true, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must not be blank");
  }

  private CatalogOptionGroup optionGroup() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
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

    return new CatalogOptionGroup(UUID.randomUUID(), product, "Stuffed Crust", 0, 1, false, true);
  }
}
