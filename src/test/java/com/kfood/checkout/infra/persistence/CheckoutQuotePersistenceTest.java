package com.kfood.checkout.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.order.domain.FulfillmentType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutQuotePersistenceTest {

  @Test
  void shouldCreateQuoteItemAndOptionSnapshots() throws Exception {
    var quote =
        new CheckoutQuote(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            FulfillmentType.DELIVERY,
            UUID.randomUUID(),
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            OffsetDateTime.now().plusMinutes(5));
    var item =
        new CheckoutQuoteItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            " Pizza Calabresa ",
            new BigDecimal("40"),
            2,
            " No onions ");
    var option =
        new CheckoutQuoteItemOption(UUID.randomUUID(), " Borda Catupiry ", new BigDecimal("8"), 1);

    item.addOption(option);
    quote.addItem(item);
    invokeValidateLifecycle(quote);

    assertThat(quote.getItems()).hasSize(1);
    assertThat(quote.getItems().getFirst().getProductNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(quote.getItems().getFirst().getUnitPriceSnapshot()).isEqualByComparingTo("40.00");
    assertThat(quote.getItems().getFirst().getNotes()).isEqualTo("No onions");
    assertThat(quote.getItems().getFirst().getOptions()).hasSize(1);
    assertThat(quote.getItems().getFirst().getOptions().getFirst().getOptionNameSnapshot())
        .isEqualTo("Borda Catupiry");
    assertThat(quote.getItems().getFirst().getOptions().getFirst().getExtraPriceSnapshot())
        .isEqualByComparingTo("8.00");
  }

  @Test
  void shouldRejectInconsistentQuoteTotal() {
    var quote =
        new CheckoutQuote(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            FulfillmentType.DELIVERY,
            UUID.randomUUID(),
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("47.99"),
            OffsetDateTime.now().plusMinutes(5));

    assertThatThrownBy(() -> invokeValidateLifecycle(quote))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("totalAmount must be equal to subtotalAmount + deliveryFeeAmount");
  }

  @Test
  void shouldRejectNullAssociationsWhenAddingItemsAndOptions() {
    var quote =
        new CheckoutQuote(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            FulfillmentType.PICKUP,
            null,
            new BigDecimal("40.00"),
            BigDecimal.ZERO,
            new BigDecimal("40.00"),
            OffsetDateTime.now().plusMinutes(5));
    var item =
        new CheckoutQuoteItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("40.00"),
            1,
            null);

    assertThatThrownBy(() -> quote.addItem(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("item is required");
    assertThatThrownBy(() -> item.addOption(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("option is required");
  }

  @Test
  void shouldInstantiateProtectedJpaConstructors() throws Exception {
    var quoteConstructor = CheckoutQuote.class.getDeclaredConstructor();
    quoteConstructor.setAccessible(true);
    var itemConstructor = CheckoutQuoteItem.class.getDeclaredConstructor();
    itemConstructor.setAccessible(true);
    var optionConstructor = CheckoutQuoteItemOption.class.getDeclaredConstructor();
    optionConstructor.setAccessible(true);

    assertThat(quoteConstructor.newInstance()).isNotNull();
    assertThat(itemConstructor.newInstance()).isNotNull();
    assertThat(optionConstructor.newInstance()).isNotNull();
  }

  @Test
  void shouldExposeQuoteItemId() {
    var item =
        new CheckoutQuoteItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("40.00"),
            1,
            null);

    assertThat(item.getId()).isNotNull();
  }

  private void invokeValidateLifecycle(CheckoutQuote quote) throws Exception {
    var method = CheckoutQuote.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);
    method.invoke(quote);
  }
}
