package com.kfood.order.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.ListOrdersQuery;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class JpaOrderQueryAdapterTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final StoreRepository storeRepository = mock(StoreRepository.class);
  private final JpaOrderQueryAdapter adapter =
      new JpaOrderQueryAdapter(salesOrderRepository, storeRepository);

  @Test
  void shouldMapOperationalOrdersPageWithoutDateFilters() {
    var storeId = UUID.randomUUID();
    var now = OffsetDateTime.parse("2026-03-22T15:00:00Z");
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
    var order = order(storeId, "PED-20260322-000123");
    var query = new ListOrdersQuery(OrderStatus.NEW, null, null, null);

    when(salesOrderRepository.findOperationalQueue(
            eq(storeId), eq(OrderStatus.NEW), eq(null), eq(null), eq(null), eq(now), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

    var response = adapter.listOperationalOrders(storeId, query, now, pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().id()).isEqualTo(order.getId());
    assertThat(response.items().getFirst().orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.items().getFirst().status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.items().getFirst().paymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.items().getFirst().customerName()).isEqualTo("Lucas Santana");
    assertThat(response.items().getFirst().totalAmount()).isEqualByComparingTo("56.50");
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.totalPages()).isEqualTo(1);
    assertThat(response.sort()).containsExactly("createdAt,desc");
  }

  @Test
  void shouldMapOperationalOrdersPageWithDateRange() {
    var storeId = UUID.randomUUID();
    var now = OffsetDateTime.parse("2026-03-22T15:00:00Z");
    var pageable =
        PageRequest.of(1, 10, Sort.by(Sort.Order.asc("status"), Sort.Order.desc("createdAt")));
    var query =
        new ListOrdersQuery(
            OrderStatus.PREPARING,
            LocalDate.of(2026, 3, 20),
            LocalDate.of(2026, 3, 22),
            FulfillmentType.DELIVERY);
    var expectedFrom = query.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
    var expectedTo = query.dateTo().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    var order = order(storeId, "PED-20260322-000456");

    when(salesOrderRepository.findOperationalQueue(
            eq(storeId),
            eq(OrderStatus.PREPARING),
            eq(FulfillmentType.DELIVERY),
            eq(expectedFrom),
            eq(expectedTo),
            eq(now),
            eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(order), pageable, 11));

    var response = adapter.listOperationalOrders(storeId, query, now, pageable);

    assertThat(response.items())
        .singleElement()
        .satisfies(item -> assertThat(item.id()).isEqualTo(order.getId()));
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(10);
    assertThat(response.totalElements()).isEqualTo(11);
    assertThat(response.totalPages()).isEqualTo(2);
    assertThat(response.sort()).containsExactly("status,asc", "createdAt,desc");
  }

  @Test
  void shouldMapDetailedOrderWithAddressAndOptions() {
    var storeId = UUID.randomUUID();
    var order = detailedOrder(storeId, true);

    when(salesOrderRepository.findDetailedByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response = adapter.getOrderDetail(storeId, order.getId());

    assertThat(response.id()).isEqualTo(order.getId());
    assertThat(response.orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.fulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);
    assertThat(response.subtotalAmount()).isEqualByComparingTo("50.00");
    assertThat(response.deliveryFeeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.totalAmount()).isEqualByComparingTo("56.50");
    assertThat(response.notes()).isEqualTo("Tocar campainha");
    assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-03-22T15:00:00Z"));
    assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-03-22T15:05:00Z"));
    assertThat(response.customer().id()).isEqualTo(order.getCustomer().getId());
    assertThat(response.customer().name()).isEqualTo("Lucas Santana");
    assertThat(response.customer().phone()).isEqualTo("21999990000");
    assertThat(response.customer().email()).isEqualTo("lucas@email.com");
    assertThat(response.payment().paymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.payment().paymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.address()).isNotNull();
    assertThat(response.address().zipCode()).isEqualTo("25000000");
    assertThat(response.address().street()).isEqualTo("Rua das Flores");
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().productNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(response.items().getFirst().unitPriceSnapshot()).isEqualByComparingTo("42.00");
    assertThat(response.items().getFirst().quantity()).isEqualTo(1);
    assertThat(response.items().getFirst().totalItemAmount()).isEqualByComparingTo("50.00");
    assertThat(response.items().getFirst().notes()).isEqualTo("Sem cebola");
    assertThat(response.items().getFirst().options()).hasSize(1);
    assertThat(response.items().getFirst().options().getFirst().optionNameSnapshot())
        .isEqualTo("Borda Catupiry");
    assertThat(response.items().getFirst().options().getFirst().extraPriceSnapshot())
        .isEqualByComparingTo("8.00");
    assertThat(response.items().getFirst().options().getFirst().quantity()).isEqualTo(1);
    assertThat(response.items().getFirst().options().getFirst().totalExtraAmount())
        .isEqualByComparingTo("8.00");
  }

  @Test
  void shouldThrowWhenOrderDetailIsMissing() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(salesOrderRepository.findDetailedByIdAndStore_Id(orderId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.getOrderDetail(storeId, orderId))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldMapDetailedOrderWithoutAddress() {
    var storeId = UUID.randomUUID();
    var order = detailedOrder(storeId, false);

    when(salesOrderRepository.findDetailedByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response = adapter.getOrderDetail(storeId, order.getId());

    assertThat(response.address()).isNull();
  }

  @Test
  void shouldMapPublicOrderLookup() {
    var store = store(UUID.randomUUID());
    var order = order(store.getId(), "PED-20260326-000123");

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(salesOrderRepository.findByStore_IdAndOrderNumber(store.getId(), "PED-20260326-000123"))
        .thenReturn(Optional.of(order));

    var response = adapter.getPublicOrderLookup("loja-do-bairro", "PED-20260326-000123");

    assertThat(response.orderNumber()).isEqualTo("PED-20260326-000123");
    assertThat(response.status()).isEqualTo(OrderStatus.NEW);
    assertThat(response.paymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(response.fulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);
    assertThat(response.subtotalAmount()).isEqualByComparingTo("50.00");
    assertThat(response.deliveryFeeAmount()).isEqualByComparingTo("6.50");
    assertThat(response.totalAmount()).isEqualByComparingTo("56.50");
    assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-03-22T15:00:00Z"));
    assertThat(response.scheduledFor()).isNull();
  }

  @Test
  void shouldThrowWhenSlugDoesNotExist() {
    when(storeRepository.findBySlug("missing-store")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.getPublicOrderLookup("missing-store", "PED-1"))
        .isInstanceOf(StoreSlugNotFoundException.class);
  }

  @Test
  void shouldThrowWhenPublicOrderDoesNotExist() {
    var store = store(UUID.randomUUID());

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(salesOrderRepository.findByStore_IdAndOrderNumber(store.getId(), "PED-404"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.getPublicOrderLookup("loja-do-bairro", "PED-404"))
        .isInstanceOf(OrderNotFoundException.class);
  }

  private Store store(UUID storeId) {
    return new Store(
        storeId,
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private SalesOrder order(UUID storeId, String orderNumber) {
    var store = store(storeId);
    var customer =
        new Customer(UUID.randomUUID(), store, "Lucas Santana", "21999990000", "lucas@email.com");
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
    order.assignOrderNumber(orderNumber);
    setCreatedAt(order, Instant.parse("2026-03-22T15:00:00Z"));
    return order;
  }

  private SalesOrder detailedOrder(UUID storeId, boolean withAddress) {
    var store = store(storeId);
    var customer =
        new Customer(UUID.randomUUID(), store, "Lucas Santana", "21999990000", "lucas@email.com");
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
            new BigDecimal("50.00"),
            new BigDecimal("6.50"),
            new BigDecimal("56.50"),
            null,
            "Tocar campainha");
    order.assignOrderNumber("PED-20260322-000123");
    if (withAddress) {
      order.defineDeliveryAddressSnapshot(address);
    }
    setCreatedAt(order, Instant.parse("2026-03-22T15:00:00Z"));
    setUpdatedAt(order, Instant.parse("2026-03-22T15:05:00Z"));

    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            "Sem cebola");
    item.addOption(
        SalesOrderItemOption.create(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));
    order.addItem(item);
    return order;
  }

  private void setCreatedAt(SalesOrder order, Instant createdAt) {
    setAuditableField(order, "createdAt", createdAt);
  }

  private void setUpdatedAt(SalesOrder order, Instant updatedAt) {
    setAuditableField(order, "updatedAt", updatedAt);
  }

  private void setAuditableField(SalesOrder order, String fieldName, Instant value) {
    try {
      var field = order.getClass().getSuperclass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(order, value);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
