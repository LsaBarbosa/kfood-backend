package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderItemTest {

  @Test
  void shouldCreateItemWithFrozenSnapshot() {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            2,
            "Sem cebola");

    item.addOption(
        SalesOrderItemOption.create(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));

    assertThat(item.getProductNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("42.00");
    assertThat(item.getQuantity()).isEqualTo(2);
    assertThat(item.getOptions()).hasSize(1);
    assertThat(item.getTotalItemAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldRejectItemWithoutProductId() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(), null, "Pizza Calabresa", new BigDecimal("42.00"), 1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("productId must not be null");
  }

  @Test
  void shouldRejectNegativeUnitPrice() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    new BigDecimal("-1.00"),
                    1,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unitPriceSnapshot must not be negative");
  }

  @Test
  void shouldRejectQuantityLessThanOne() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    new BigDecimal("42.00"),
                    0,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("quantity must be greater than zero");
  }
}
