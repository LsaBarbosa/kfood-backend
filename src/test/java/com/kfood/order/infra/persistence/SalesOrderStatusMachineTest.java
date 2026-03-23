package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderStatusMachineTest {

  @Test
  void shouldAllowTransitionFromNewToPreparing() {
    var order = order(FulfillmentType.DELIVERY);

    assertThat(order.canTransitionTo(OrderStatus.PREPARING)).isTrue();

    order.changeStatus(OrderStatus.PREPARING);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(order.isFinalStatus()).isFalse();
  }

  @Test
  void shouldRejectNullTargetStatus() {
    var order = order(FulfillmentType.DELIVERY);

    assertThatThrownBy(() -> order.canTransitionTo(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("targetStatus must not be null");
  }

  @Test
  void shouldRejectInvalidTransitionFromNewToReady() {
    var order = order(FulfillmentType.DELIVERY);

    assertThat(order.canTransitionTo(OrderStatus.READY)).isFalse();
    assertThatThrownBy(() -> order.changeStatus(OrderStatus.READY))
        .isInstanceOf(OrderStatusTransitionException.class)
        .hasMessage("Invalid order status transition from NEW to READY");
  }

  @Test
  void shouldAllowTransitionFromPreparingToReady() {
    var order = order(FulfillmentType.DELIVERY);
    order.changeStatus(OrderStatus.PREPARING);

    assertThat(order.canTransitionTo(OrderStatus.READY)).isTrue();

    order.changeStatus(OrderStatus.READY);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
  }

  @Test
  void shouldAllowDeliveryOrderToMoveFromReadyToOutForDelivery() {
    var order = order(FulfillmentType.DELIVERY);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);

    assertThat(order.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isTrue();

    order.changeStatus(OrderStatus.OUT_FOR_DELIVERY);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
    assertThat(order.isFinalStatus()).isFalse();
  }

  @Test
  void shouldAllowPickupOrderToMoveFromReadyToCompleted() {
    var order = order(FulfillmentType.PICKUP);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);

    assertThat(order.canTransitionTo(OrderStatus.COMPLETED)).isTrue();

    order.changeStatus(OrderStatus.COMPLETED);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    assertThat(order.isFinalStatus()).isTrue();
    assertThat(order.canTransitionTo(OrderStatus.CANCELED)).isFalse();
  }

  @Test
  void shouldRejectOutForDeliveryForPickupOrder() {
    var order = order(FulfillmentType.PICKUP);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);

    assertThat(order.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isFalse();
    assertThatThrownBy(() -> order.changeStatus(OrderStatus.OUT_FOR_DELIVERY))
        .isInstanceOf(OrderStatusTransitionException.class)
        .hasMessage("Invalid order status transition from READY to OUT_FOR_DELIVERY");
  }

  @Test
  void shouldAllowOutForDeliveryToCompleted() {
    var order = order(FulfillmentType.DELIVERY);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);
    order.changeStatus(OrderStatus.OUT_FOR_DELIVERY);

    assertThat(order.canTransitionTo(OrderStatus.COMPLETED)).isTrue();

    order.changeStatus(OrderStatus.COMPLETED);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    assertThat(order.isFinalStatus()).isTrue();
  }

  @Test
  void shouldNotReturnCanceledOrderToOperationalFlow() {
    var order = order(FulfillmentType.DELIVERY);

    assertThat(order.canTransitionTo(OrderStatus.CANCELED)).isTrue();

    order.changeStatus(OrderStatus.CANCELED);

    assertThatThrownBy(() -> order.changeStatus(OrderStatus.PREPARING))
        .isInstanceOf(OrderStatusTransitionException.class)
        .hasMessage("Invalid order status transition from CANCELED to PREPARING");

    assertThat(order.isFinalStatus()).isTrue();
    assertThat(order.canTransitionTo(OrderStatus.READY)).isFalse();
  }

  @Test
  void shouldCancelUsingExplicitIntentMethod() {
    var order = order(FulfillmentType.DELIVERY);

    order.cancel();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(order.isFinalStatus()).isTrue();
  }

  @Test
  void shouldAllowCancelFromPreparingReadyAndOutForDelivery() {
    var preparing = order(FulfillmentType.DELIVERY);
    preparing.changeStatus(OrderStatus.PREPARING);
    preparing.changeStatus(OrderStatus.CANCELED);
    assertThat(preparing.getStatus()).isEqualTo(OrderStatus.CANCELED);

    var readyDelivery = order(FulfillmentType.DELIVERY);
    readyDelivery.changeStatus(OrderStatus.PREPARING);
    readyDelivery.changeStatus(OrderStatus.READY);
    readyDelivery.changeStatus(OrderStatus.CANCELED);
    assertThat(readyDelivery.getStatus()).isEqualTo(OrderStatus.CANCELED);

    var outForDelivery = order(FulfillmentType.DELIVERY);
    outForDelivery.changeStatus(OrderStatus.PREPARING);
    outForDelivery.changeStatus(OrderStatus.READY);
    outForDelivery.changeStatus(OrderStatus.OUT_FOR_DELIVERY);
    outForDelivery.changeStatus(OrderStatus.CANCELED);
    assertThat(outForDelivery.getStatus()).isEqualTo(OrderStatus.CANCELED);
  }

  @Test
  void shouldRejectOtherTransitionsFromPreparingReadyAndOutForDelivery() {
    var preparing = order(FulfillmentType.DELIVERY);
    preparing.changeStatus(OrderStatus.PREPARING);
    assertThat(preparing.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isFalse();

    var readyDelivery = order(FulfillmentType.DELIVERY);
    readyDelivery.changeStatus(OrderStatus.PREPARING);
    readyDelivery.changeStatus(OrderStatus.READY);
    assertThat(readyDelivery.canTransitionTo(OrderStatus.COMPLETED)).isFalse();

    var readyPickup = order(FulfillmentType.PICKUP);
    readyPickup.changeStatus(OrderStatus.PREPARING);
    readyPickup.changeStatus(OrderStatus.READY);
    assertThat(readyPickup.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isFalse();

    var outForDelivery = order(FulfillmentType.DELIVERY);
    outForDelivery.changeStatus(OrderStatus.PREPARING);
    outForDelivery.changeStatus(OrderStatus.READY);
    outForDelivery.changeStatus(OrderStatus.OUT_FOR_DELIVERY);
    assertThat(outForDelivery.canTransitionTo(OrderStatus.READY)).isFalse();
  }

  @Test
  void shouldRejectCancelingCompletedOrderUsingIntentMethod() {
    var order = order(FulfillmentType.PICKUP);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);
    order.changeStatus(OrderStatus.COMPLETED);

    assertThatThrownBy(order::cancel)
        .isInstanceOf(OrderStatusTransitionException.class)
        .hasMessage("Invalid order status transition from COMPLETED to CANCELED");
  }

  private SalesOrder order(FulfillmentType fulfillmentType) {
    var store = mock(Store.class);
    var customer = mock(Customer.class);

    return SalesOrder.create(
        UUID.randomUUID(),
        store,
        customer,
        fulfillmentType,
        PaymentMethod.PIX,
        new BigDecimal("42.00"),
        fulfillmentType == FulfillmentType.DELIVERY ? new BigDecimal("8.00") : BigDecimal.ZERO,
        fulfillmentType == FulfillmentType.DELIVERY
            ? new BigDecimal("50.00")
            : new BigDecimal("42.00"),
        null,
        null);
  }
}
