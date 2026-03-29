package com.kfood.order.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.app.CancelOrderCommand;
import com.kfood.order.app.UpdateOrderStatusCommand;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.OrderStatusHistory;
import com.kfood.order.infra.persistence.OrderStatusHistoryRepository;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.exceptions.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaOrderWorkflowAdapterTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final OrderStatusHistoryRepository orderStatusHistoryRepository =
      mock(OrderStatusHistoryRepository.class);
  private final JpaOrderWorkflowAdapter adapter =
      new JpaOrderWorkflowAdapter(salesOrderRepository, orderStatusHistoryRepository);

  @Test
  void shouldUpdateOrderStatusAndPersistHistory() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var order = order(storeId, FulfillmentType.DELIVERY);

    when(salesOrderRepository.findByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response =
        adapter.updateStatus(
            storeId,
            actorUserId,
            order.getId(),
            new UpdateOrderStatusCommand(OrderStatus.PREPARING, "validated"),
            Instant.parse("2026-03-22T18:40:00Z"));

    assertThat(response.previousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(response.newStatus()).isEqualTo(OrderStatus.PREPARING);
    verify(orderStatusHistoryRepository).saveAndFlush(any(OrderStatusHistory.class));
  }

  @Test
  void shouldRejectCanceledOnUpdateStatusEndpoint() {
    assertThatThrownBy(
            () ->
                adapter.updateStatus(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new UpdateOrderStatusCommand(OrderStatus.CANCELED, "invalid"),
                    Instant.now()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Use the cancel endpoint to move an order to CANCELED.");
  }

  @Test
  void shouldCancelOrderAndPersistHistory() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var order = order(storeId, FulfillmentType.PICKUP);

    when(salesOrderRepository.findByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response =
        adapter.cancel(
            storeId,
            actorUserId,
            order.getId(),
            new CancelOrderCommand("customer request"),
            Instant.parse("2026-03-22T19:10:00Z"));

    assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    assertThat(response.reason()).isEqualTo("customer request");
    verify(orderStatusHistoryRepository).saveAndFlush(any(OrderStatusHistory.class));
  }

  @Test
  void shouldRejectBlankCancelReason() {
    assertThatThrownBy(
            () ->
                adapter.cancel(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new CancelOrderCommand(" "),
                    Instant.now()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Cancellation reason must not be blank.");
  }

  private SalesOrder order(UUID storeId, FulfillmentType fulfillmentType) {
    var store =
        new Store(
            storeId,
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var customer =
        new Customer(UUID.randomUUID(), store, "Lucas Santana", "21999990000", "lucas@email.com");
    return SalesOrder.create(
        UUID.randomUUID(),
        store,
        customer,
        fulfillmentType,
        PaymentMethod.PIX,
        new BigDecimal("50.00"),
        fulfillmentType == FulfillmentType.DELIVERY ? new BigDecimal("6.50") : BigDecimal.ZERO,
        fulfillmentType == FulfillmentType.DELIVERY
            ? new BigDecimal("56.50")
            : new BigDecimal("50.00"),
        null,
        null);
  }
}
