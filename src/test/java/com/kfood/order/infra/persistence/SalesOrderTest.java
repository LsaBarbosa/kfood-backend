package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderTest {

  @Test
  void shouldCreateOrderWithInitialStatusNew() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            "No onions");

    assertThat(order.getStore()).isEqualTo(store);
    assertThat(order.getCustomer()).isEqualTo(customer);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.getSubtotalAmount()).isEqualByComparingTo("40.00");
    assertThat(order.getDeliveryFeeAmount()).isEqualByComparingTo("8.00");
    assertThat(order.getTotalAmount()).isEqualByComparingTo("48.00");
    assertThat(order.getNotes()).isEqualTo("No onions");
  }

  @Test
  void shouldRejectOrderWithoutStore() {
    var customer = mock(Customer.class);

    assertThatThrownBy(
            () ->
                SalesOrder.create(
                    UUID.randomUUID(),
                    null,
                    customer,
                    FulfillmentType.DELIVERY,
                    PaymentMethod.PIX,
                    new BigDecimal("40.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("48.00"),
                    null,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("store must not be null");
  }

  @Test
  void shouldRejectOrderWithoutCustomer() {
    var store = mock(Store.class);

    assertThatThrownBy(
            () ->
                SalesOrder.create(
                    UUID.randomUUID(),
                    store,
                    null,
                    FulfillmentType.DELIVERY,
                    PaymentMethod.PIX,
                    new BigDecimal("40.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("48.00"),
                    null,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("customer must not be null");
  }

  @Test
  void shouldRejectOrderWhenTotalIsInconsistent() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);

    assertThatThrownBy(
            () ->
                SalesOrder.create(
                    UUID.randomUUID(),
                    store,
                    customer,
                    FulfillmentType.DELIVERY,
                    PaymentMethod.PIX,
                    new BigDecimal("40.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("47.99"),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("totalAmount must be equal to subtotalAmount + deliveryFeeAmount");
  }

  @Test
  void shouldRejectReassigningOrderNumber() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    when(store.getTimezone()).thenReturn("America/Sao_Paulo");

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            null);
    order.assignOrderNumber("PED-20260321-000001");

    assertThatThrownBy(() -> order.assignOrderNumber("PED-20260321-000002"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("orderNumber is already assigned");
  }

  @Test
  void shouldAllowUpdatingPaymentMethodSnapshot() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            null);

    order.markPaymentMethodSnapshot(PaymentMethod.CASH);

    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.CASH);
  }

  @Test
  void shouldRejectNullPaymentMethodSnapshot() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            null);

    assertThatThrownBy(() -> order.markPaymentMethodSnapshot(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("paymentMethod must not be null");
  }

  @Test
  void shouldAcceptFutureScheduleAndKeepStatusNew() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var fixedClock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
    var futureSchedule = OffsetDateTime.parse("2026-03-21T16:00:00Z");

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            "Scheduled order");

    order.defineSchedule(futureSchedule, fixedClock);

    assertThat(order.getScheduledFor()).isEqualTo(futureSchedule);
    assertThat(order.isScheduled()).isTrue();
    assertThat(order.isScheduledForFuture(fixedClock)).isTrue();
    assertThat(order.isAvailableForOperation(fixedClock)).isFalse();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
  }

  @Test
  void shouldRejectPastSchedule() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var fixedClock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
    var pastSchedule = OffsetDateTime.parse("2026-03-21T14:00:00Z");

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            "Scheduled order");

    assertThatThrownBy(() -> order.defineSchedule(pastSchedule, fixedClock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("scheduledFor must be in the future");
  }

  @Test
  void shouldMakeScheduledOrderAvailableForOperationWhenTimeArrives() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var fixedClock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
    var afterScheduleClock = Clock.fixed(Instant.parse("2026-03-21T16:05:00Z"), ZoneOffset.UTC);
    var scheduledFor = OffsetDateTime.parse("2026-03-21T16:00:00Z");

    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            BigDecimal.ZERO,
            new BigDecimal("40.00"),
            null,
            null);

    order.defineSchedule(scheduledFor, fixedClock);

    assertThat(order.isScheduledForFuture(afterScheduleClock)).isFalse();
    assertThat(order.isAvailableForOperation(afterScheduleClock)).isTrue();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
  }

  @Test
  void shouldDefineDeliveryAddressSnapshot() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var address =
        new CustomerAddress(
            UUID.randomUUID(),
            customer,
            "Casa",
            "25000000",
            "Rua das Flores",
            "45",
            "Centro",
            "Mage",
            "RJ",
            "Ap 101",
            true);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            null);

    order.defineDeliveryAddressSnapshot(address);

    assertThat(order.hasDeliveryAddressSnapshot()).isTrue();
    assertThat(order.getDeliveryAddressLabel()).isEqualTo("Casa");
    assertThat(order.getDeliveryAddressStreet()).isEqualTo("Rua das Flores");
    assertThat(order.getDeliveryAddressState()).isEqualTo("RJ");
    assertThat(order.getDeliveryAddressComplement()).isEqualTo("Ap 101");
  }

  @Test
  void shouldRejectNullDeliveryAddressSnapshot() {
    var order = createPickupOrder();

    assertThatThrownBy(() -> order.defineDeliveryAddressSnapshot(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("address must not be null");
  }

  @Test
  void shouldRejectBlankOrderNumber() {
    var order = createPickupOrder();

    assertThatThrownBy(() -> order.assignOrderNumber(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("orderNumber must not be blank");
  }

  @Test
  void shouldAllowClearingSchedule() {
    var order = createPickupOrder();
    var fixedClock = Clock.fixed(Instant.parse("2026-03-21T15:00:00Z"), ZoneOffset.UTC);
    order.defineSchedule(OffsetDateTime.parse("2026-03-21T16:00:00Z"), fixedClock);

    order.defineSchedule(null, fixedClock);

    assertThat(order.getScheduledFor()).isNull();
    assertThat(order.isScheduled()).isFalse();
  }

  @Test
  void shouldRejectNullPaymentStatusSnapshot() {
    var order = createPickupOrder();

    assertThatThrownBy(() -> order.markPaymentStatusSnapshot(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("paymentStatusSnapshot must not be null");
  }

  @Test
  void shouldUpdatePaymentStatusSnapshot() {
    var order = createPickupOrder();

    order.markPaymentStatusSnapshot(com.kfood.payment.domain.PaymentStatusSnapshot.PAID);

    assertThat(order.getPaymentStatusSnapshot())
        .isEqualTo(com.kfood.payment.domain.PaymentStatusSnapshot.PAID);
  }

  @Test
  void shouldAllowTransitionFromPendingToFailed() {
    var order = createPickupOrder();

    order.markPaymentStatusSnapshot(com.kfood.payment.domain.PaymentStatusSnapshot.FAILED);

    assertThat(order.getPaymentStatusSnapshot())
        .isEqualTo(com.kfood.payment.domain.PaymentStatusSnapshot.FAILED);
  }

  @Test
  void shouldRejectRegressionFromPaidToPending() {
    var order = createPickupOrder();
    order.markPaymentStatusSnapshot(com.kfood.payment.domain.PaymentStatusSnapshot.PAID);

    assertThatThrownBy(
            () ->
                order.markPaymentStatusSnapshot(
                    com.kfood.payment.domain.PaymentStatusSnapshot.PENDING))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("paymentStatusSnapshot cannot regress from PAID");
  }

  @Test
  void shouldRejectNegativeSubtotal() {
    var store = mock(Store.class);
    var customer = mock(Customer.class);

    assertThatThrownBy(
            () ->
                SalesOrder.create(
                    UUID.randomUUID(),
                    store,
                    customer,
                    FulfillmentType.PICKUP,
                    PaymentMethod.PIX,
                    new BigDecimal("-1.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("-1.00"),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("subtotalAmount must not be negative");
  }

  @Test
  void shouldRejectNegativeDeliveryFee() {
    assertThatThrownBy(
            () ->
                SalesOrder.create(
                    UUID.randomUUID(),
                    mock(Store.class),
                    mock(Customer.class),
                    FulfillmentType.DELIVERY,
                    PaymentMethod.PIX,
                    new BigDecimal("40.00"),
                    new BigDecimal("-1.00"),
                    new BigDecimal("39.00"),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("deliveryFeeAmount must not be negative");
  }

  @Test
  void shouldRejectNegativeTotalAmount() {
    assertThatThrownBy(
            () ->
                SalesOrder.create(
                    UUID.randomUUID(),
                    mock(Store.class),
                    mock(Customer.class),
                    FulfillmentType.PICKUP,
                    PaymentMethod.PIX,
                    new BigDecimal("40.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("-1.00"),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("totalAmount must not be negative");
  }

  @Test
  void shouldExposeOrderMetadata() {
    var order = createPickupOrder();

    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.getFulfillmentType()).isEqualTo(FulfillmentType.PICKUP);
    assertThat(order.isScheduledForFuture(Clock.systemUTC())).isFalse();
    assertThat(order.isAvailableForOperation(Clock.systemUTC())).isTrue();
  }

  private SalesOrder createPickupOrder() {
    return SalesOrder.create(
        UUID.randomUUID(),
        mock(Store.class),
        mock(Customer.class),
        FulfillmentType.PICKUP,
        PaymentMethod.PIX,
        new BigDecimal("40.00"),
        BigDecimal.ZERO,
        new BigDecimal("40.00"),
        null,
        null);
  }
}
