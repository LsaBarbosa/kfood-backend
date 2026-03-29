package com.kfood.order.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.customer.infra.persistence.CustomerAddress;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderItem;
import com.kfood.order.infra.persistence.SalesOrderItemOption;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
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
  void shouldMapOperationalOrdersPage() {
    var storeId = UUID.randomUUID();
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
    var order = order(storeId, "PED-20260322-000123");
    var query = new com.kfood.order.app.ListOrdersQuery(OrderStatus.NEW, null, null, null);

    when(salesOrderRepository.findOperationalQueue(
            eq(storeId), eq(OrderStatus.NEW), eq(null), eq(null), eq(null), any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

    var response =
        adapter.listOperationalOrders(
            storeId, query, OffsetDateTime.parse("2026-03-22T15:00:00Z"), pageable);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.sort()).containsExactly("createdAt,desc");
  }

  @Test
  void shouldMapDetailedOrder() {
    var storeId = UUID.randomUUID();
    var order = detailedOrder(storeId);

    when(salesOrderRepository.findDetailedByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response = adapter.getOrderDetail(storeId, order.getId());

    assertThat(response.orderNumber()).isEqualTo("PED-20260322-000123");
    assertThat(response.address().zipCode()).isEqualTo("25000000");
    assertThat(response.items().getFirst().options().getFirst().optionNameSnapshot())
        .isEqualTo("Borda Catupiry");
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
  void shouldMapPublicOrderLookup() {
    var store = store(UUID.randomUUID());
    var order = order(store.getId(), "PED-20260326-000123");

    when(storeRepository.findBySlug("loja-do-bairro")).thenReturn(Optional.of(store));
    when(salesOrderRepository.findByStore_IdAndOrderNumber(store.getId(), "PED-20260326-000123"))
        .thenReturn(Optional.of(order));

    var response = adapter.getPublicOrderLookup("loja-do-bairro", "PED-20260326-000123");

    assertThat(response.orderNumber()).isEqualTo("PED-20260326-000123");
    assertThat(response.totalAmount()).isEqualByComparingTo("56.50");
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
    return order;
  }

  private SalesOrder detailedOrder(UUID storeId) {
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
    order.defineDeliveryAddressSnapshot(address);
    setCreatedAt(order);

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

  private void setCreatedAt(SalesOrder order) {
    try {
      var field = order.getClass().getSuperclass().getDeclaredField("createdAt");
      field.setAccessible(true);
      field.set(order, Instant.parse("2026-03-22T15:00:00Z"));
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
