package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderItemOptionTest {

  @Test
  void shouldCreateOptionSnapshot() {
    var option =
        SalesOrderItemOption.create(UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 2);

    assertThat(option.getOptionNameSnapshot()).isEqualTo("Borda Catupiry");
    assertThat(option.getExtraPriceSnapshot()).isEqualByComparingTo("8.00");
    assertThat(option.getQuantity()).isEqualTo(2);
    assertThat(option.getTotalExtraAmount()).isEqualByComparingTo("16.00");
  }

  @Test
  void shouldRejectNegativeExtraPrice() {
    assertThatThrownBy(
            () ->
                SalesOrderItemOption.create(
                    UUID.randomUUID(), "Borda Catupiry", new BigDecimal("-1.00"), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraPriceSnapshot must not be negative");
  }

  @Test
  void shouldRejectBlankOptionName() {
    assertThatThrownBy(
            () -> SalesOrderItemOption.create(UUID.randomUUID(), " ", new BigDecimal("8.00"), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("optionNameSnapshot must not be blank");
  }

  @Test
  void shouldRejectQuantityLessThanOne() {
    assertThatThrownBy(
            () ->
                SalesOrderItemOption.create(UUID.randomUUID(), "Borda", new BigDecimal("8.00"), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("quantity must be greater than zero");
  }
}
