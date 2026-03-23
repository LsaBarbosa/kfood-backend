package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AssignOrderNumberServiceTest {

  @Test
  void shouldAssignNumberWhenMissing() {
    var generator = mock(OrderNumberGenerator.class);
    var service = new AssignOrderNumberService(generator);
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            null);
    when(generator.next(order)).thenReturn("PED-20260321-000001");

    service.assignIfMissing(order);

    assertThat(order.getOrderNumber()).isEqualTo("PED-20260321-000001");
    verify(generator).next(order);
  }

  @Test
  void shouldNotReassignWhenAlreadyPresent() {
    var generator = mock(OrderNumberGenerator.class);
    var service = new AssignOrderNumberService(generator);
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            null);
    order.assignOrderNumber("PED-20260321-000001");

    service.assignIfMissing(order);

    assertThat(order.getOrderNumber()).isEqualTo("PED-20260321-000001");
    verifyNoInteractions(generator);
  }

  @Test
  void shouldAssignWhenExistingOrderNumberIsBlank() {
    var generator = mock(OrderNumberGenerator.class);
    var service = new AssignOrderNumberService(generator);
    var store = mock(Store.class);
    var customer = mock(Customer.class);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            null);
    ReflectionTestUtils.setField(order, "orderNumber", " ");
    when(generator.next(order)).thenReturn("PED-20260321-000002");

    service.assignIfMissing(order);

    assertThat(order.getOrderNumber()).isEqualTo("PED-20260321-000002");
  }
}
