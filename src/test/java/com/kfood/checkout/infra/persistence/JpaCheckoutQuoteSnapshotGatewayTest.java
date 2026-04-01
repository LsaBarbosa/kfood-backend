package com.kfood.checkout.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.checkout.app.CheckoutQuoteItemSnapshot;
import com.kfood.checkout.app.CheckoutQuoteOptionSnapshot;
import com.kfood.checkout.app.CheckoutQuoteSnapshot;
import com.kfood.order.domain.FulfillmentType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaCheckoutQuoteSnapshotGatewayTest {

  private final CheckoutQuoteRepository checkoutQuoteRepository =
      mock(CheckoutQuoteRepository.class);
  private final JpaCheckoutQuoteSnapshotGateway gateway =
      new JpaCheckoutQuoteSnapshotGateway(checkoutQuoteRepository);

  @Test
  void shouldSaveAndMapQuoteSnapshot() {
    var snapshot = snapshot();
    when(checkoutQuoteRepository.save(any(CheckoutQuote.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var saved = gateway.save(snapshot);

    assertThat(saved.quoteId()).isEqualTo(snapshot.quoteId());
    assertThat(saved.storeId()).isEqualTo(snapshot.storeId());
    assertThat(saved.customerId()).isEqualTo(snapshot.customerId());
    assertThat(saved.fulfillmentType()).isEqualTo(snapshot.fulfillmentType());
    assertThat(saved.items()).hasSize(1);
    assertThat(saved.items().getFirst().productNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(saved.items().getFirst().options()).hasSize(1);
    assertThat(saved.items().getFirst().options().getFirst().optionNameSnapshot())
        .isEqualTo("Borda Catupiry");
  }

  @Test
  void shouldReturnMappedValidSnapshot() {
    var entity =
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
            "Pizza Calabresa",
            new BigDecimal("40.00"),
            1,
            "No onions");
    item.addOption(
        new CheckoutQuoteItemOption(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));
    entity.addItem(item);
    when(checkoutQuoteRepository.findByIdAndStoreIdAndExpiresAtAfter(
            eq(entity.getId()), eq(entity.getStoreId()), any(OffsetDateTime.class)))
        .thenReturn(Optional.of(entity));

    var found = gateway.findValidByStoreIdAndQuoteId(entity.getStoreId(), entity.getId());

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().totalAmount()).isEqualByComparingTo("48.00");
    assertThat(found.orElseThrow().items().getFirst().notes()).isEqualTo("No onions");
  }

  @Test
  void shouldReturnEmptyWhenQuoteIsNotFound() {
    var storeId = UUID.randomUUID();
    var quoteId = UUID.randomUUID();
    when(checkoutQuoteRepository.findByIdAndStoreIdAndExpiresAtAfter(
            eq(quoteId), eq(storeId), any(OffsetDateTime.class)))
        .thenReturn(Optional.empty());

    var found = gateway.findValidByStoreIdAndQuoteId(storeId, quoteId);

    assertThat(found).isEmpty();
    verify(checkoutQuoteRepository)
        .findByIdAndStoreIdAndExpiresAtAfter(eq(quoteId), eq(storeId), any(OffsetDateTime.class));
  }

  private CheckoutQuoteSnapshot snapshot() {
    return new CheckoutQuoteSnapshot(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        FulfillmentType.DELIVERY,
        UUID.randomUUID(),
        new BigDecimal("40.00"),
        new BigDecimal("8.00"),
        new BigDecimal("48.00"),
        List.of(
            new CheckoutQuoteItemSnapshot(
                UUID.randomUUID(),
                "Pizza Calabresa",
                new BigDecimal("40.00"),
                1,
                "No onions",
                List.of(
                    new CheckoutQuoteOptionSnapshot("Borda Catupiry", new BigDecimal("8.00"), 1)))),
        OffsetDateTime.now().plusMinutes(5));
  }
}
