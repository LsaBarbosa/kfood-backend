package com.kfood.order.infra.numbering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DatabaseOrderNumberGeneratorTest {

  @Test
  void shouldGenerateOrderNumberWithExpectedFormat() {
    var entityManager = mock(EntityManager.class);
    var query = mock(Query.class);
    when(entityManager.createNativeQuery("select nextval('sales_order_number_seq')"))
        .thenReturn(query);
    when(query.getSingleResult()).thenReturn(123L);

    var generator =
        new DatabaseOrderNumberGenerator(
            Clock.fixed(Instant.parse("2026-03-21T15:30:00Z"), ZoneId.of("UTC")));
    ReflectionTestUtils.setField(generator, "entityManager", entityManager);

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
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            null);

    var orderNumber = generator.next(order);

    assertThat(orderNumber).matches("^PED-[0-9]{8}-[0-9]{6}$");
    assertThat(orderNumber).endsWith("-000123");
    assertThat(orderNumber).startsWith("PED-20260321-");
  }

  @Test
  void shouldUseDefaultTimezoneWhenStoreTimezoneIsBlank() {
    var entityManager = mock(EntityManager.class);
    var query = mock(Query.class);
    when(entityManager.createNativeQuery("select nextval('sales_order_number_seq')"))
        .thenReturn(query);
    when(query.getSingleResult()).thenReturn(1L);

    var generator =
        new DatabaseOrderNumberGenerator(
            Clock.fixed(Instant.parse("2026-03-21T03:30:00Z"), ZoneId.of("UTC")));
    ReflectionTestUtils.setField(generator, "entityManager", entityManager);

    var store = mock(Store.class);
    var customer = mock(Customer.class);
    when(store.getTimezone()).thenReturn(" ");
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

    assertThat(generator.next(order)).startsWith("PED-20260321-");
  }
}
