package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import java.math.BigDecimal;
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
            new BigDecimal("40.00"),
            new BigDecimal("8.00"),
            new BigDecimal("48.00"),
            null,
            "No onions");

    assertThat(order.getStore()).isEqualTo(store);
    assertThat(order.getCustomer()).isEqualTo(customer);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
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
                    new BigDecimal("40.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("47.99"),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("totalAmount must be equal to subtotalAmount + deliveryFeeAmount");
  }
}
