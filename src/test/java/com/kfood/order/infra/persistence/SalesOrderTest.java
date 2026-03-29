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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    var storeId = UUID.randomUUID();

    when(store.getId()).thenReturn(storeId);
    when(store.isCashPaymentEnabled()).thenReturn(true);

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
            "  No onions  ");

    assertThat(order.getStore()).isEqualTo(store);
    assertThat(order.getStoreId()).isEqualTo(storeId);
    assertThat(order.getCustomer()).isEqualTo(customer);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.isCashPaymentEnabled()).isTrue();
    assertThat(order.getSubtotalAmount()).isEqualByComparingTo("40.00");
    assertThat(order.getDeliveryFeeAmount()).isEqualByComparingTo("8.00");
    assertThat(order.getTotalAmount()).isEqualByComparingTo("48.00");
    assertThat(order.getNotes()).isEqualTo("No onions");
  }

  @Test
  void shouldNormalizeBlankNotesToNull() {
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            mock(Store.class),
            mock(Customer.class),
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("40.00"),
            BigDecimal.ZERO,
            new BigDecimal("40.00"),
            null,
            "   ");

    assertThat(order.getNotes()).isNull();
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
    assertThat(order.getDeliveryAddressZipCode()).isEqualTo("25000000");
    assertThat(order.getDeliveryAddressStreet()).isEqualTo("Rua das Flores");
    assertThat(order.getDeliveryAddressState()).isEqualTo("RJ");
    assertThat(order.getDeliveryAddressComplement()).isEqualTo("Ap 101");
  }

  @Test
  void shouldExposeItemsAsUnmodifiableList() {
    var order = createPickupOrder();
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("40.00"),
            1,
            null);

    order.addItem(item);

    assertThat(order.getItems()).containsExactly(item);
    assertThatThrownBy(() -> order.getItems().add(item))
        .isInstanceOf(UnsupportedOperationException.class);
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

  @Test
  void shouldCoverLifecycleValidationBranchesUsedByJpa() throws Exception {
    var order = instantiateJpaOrder();

    setField(order, "subtotalAmount", new BigDecimal("40.00"));
    setField(order, "deliveryFeeAmount", BigDecimal.ZERO);
    setField(order, "totalAmount", new BigDecimal("40.00"));
    setField(order, "paymentMethod", null);
    setField(order, "paymentMethodSnapshot", PaymentMethod.CASH);

    invokeValidateLifecycle(order);
    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);

    setField(order, "paymentMethod", PaymentMethod.PIX);
    setField(order, "paymentMethodSnapshot", null);

    invokeValidateLifecycle(order);
    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);

    setField(order, "paymentMethod", PaymentMethod.PIX);
    setField(order, "paymentMethodSnapshot", PaymentMethod.CASH);

    invokeValidateLifecycle(order);
    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
  }

  @Test
  void shouldCoverRemainingStatusTransitionsAndFinalStates() {
    var deliveryOrder = createDeliveryOrder();

    assertThat(deliveryOrder.canTransitionTo(OrderStatus.CANCELED)).isTrue();
    deliveryOrder.changeStatus(OrderStatus.PREPARING);
    assertThat(deliveryOrder.canTransitionTo(OrderStatus.CANCELED)).isTrue();
    deliveryOrder.changeStatus(OrderStatus.READY);
    assertThat(deliveryOrder.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isTrue();
    assertThat(deliveryOrder.canTransitionTo(OrderStatus.CANCELED)).isTrue();
    deliveryOrder.changeStatus(OrderStatus.OUT_FOR_DELIVERY);
    assertThat(deliveryOrder.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
    assertThat(deliveryOrder.canTransitionTo(OrderStatus.CANCELED)).isTrue();
    deliveryOrder.changeStatus(OrderStatus.COMPLETED);

    assertThat(deliveryOrder.isFinalStatus()).isTrue();

    var canceledOrder = createPickupOrder();
    canceledOrder.cancel();
    assertThat(canceledOrder.isFinalStatus()).isTrue();
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

  private SalesOrder createDeliveryOrder() {
    return SalesOrder.create(
        UUID.randomUUID(),
        mock(Store.class),
        mock(Customer.class),
        FulfillmentType.DELIVERY,
        PaymentMethod.PIX,
        new BigDecimal("40.00"),
        new BigDecimal("8.00"),
        new BigDecimal("48.00"),
        null,
        null);
  }

  private SalesOrder instantiateJpaOrder() throws Exception {
    var constructor = SalesOrder.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private void invokeValidateLifecycle(SalesOrder order) throws Exception {
    Method method = SalesOrder.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);
    method.invoke(order);
  }

  private void setField(SalesOrder order, String fieldName, Object value) throws Exception {
    Field field = SalesOrder.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(order, value);
  }
}
