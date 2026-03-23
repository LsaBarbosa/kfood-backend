package com.kfood.checkout.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutQuoteItemTest {

  @Test
  void shouldNormalizeBlankNotesToNull() {
    var item =
        new CheckoutQuoteItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("39.9"),
            2,
            "   ");

    assertThat(item.getNotes()).isNull();
    assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("39.90");
  }

  @Test
  void shouldTrimNotesWhenProvided() {
    var item =
        new CheckoutQuoteItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("39.9"),
            2,
            "  sem cebola  ");

    assertThat(item.getNotes()).isEqualTo("sem cebola");
  }
}
