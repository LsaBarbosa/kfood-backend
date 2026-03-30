package com.kfood.order.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.app.CancelOrderCommand;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.app.UpdateOrderStatusCommand;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.OrderStatusHistory;
import com.kfood.order.infra.persistence.OrderStatusHistoryRepository;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class JpaOrderWorkflowAdapterTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final OrderStatusHistoryRepository orderStatusHistoryRepository =
      mock(OrderStatusHistoryRepository.class);
  private final JpaOrderWorkflowAdapter adapter =
      new JpaOrderWorkflowAdapter(salesOrderRepository, orderStatusHistoryRepository);

  @Test
  void shouldUpdateStatusWithSuccess() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var changedAt = Instant.parse("2026-03-22T18:40:00Z");
    var order = order(storeId, FulfillmentType.DELIVERY);

    when(salesOrderRepository.findByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response =
        adapter.updateStatus(
            storeId,
            actorUserId,
            order.getId(),
            new UpdateOrderStatusCommand(OrderStatus.PREPARING, "validated"),
            changedAt);

    assertThat(response.id()).isEqualTo(order.getId());
    assertThat(response.previousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(response.newStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(response.changedAt()).isEqualTo(changedAt);
    assertThat(response.changedBy()).isEqualTo(actorUserId);
    verify(salesOrderRepository).saveAndFlush(order);
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
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getMessage())
                  .isEqualTo("Use the cancel endpoint to move an order to CANCELED.");
            });

    verifyNoInteractions(salesOrderRepository, orderStatusHistoryRepository);
  }

  @Test
  void shouldThrowWhenOrderIsMissingOnUpdateStatus() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(salesOrderRepository.findByIdAndStore_Id(orderId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.updateStatus(
                    storeId,
                    UUID.randomUUID(),
                    orderId,
                    new UpdateOrderStatusCommand(OrderStatus.PREPARING, "validated"),
                    Instant.now()))
        .isInstanceOf(OrderNotFoundException.class);

    verify(salesOrderRepository).findByIdAndStore_Id(orderId, storeId);
    verify(salesOrderRepository, never()).saveAndFlush(any());
    verifyNoInteractions(orderStatusHistoryRepository);
  }

  @Test
  void shouldConvertInvalidTransitionOnUpdateStatus() {
    var storeId = UUID.randomUUID();
    var order = order(storeId, FulfillmentType.DELIVERY);

    when(salesOrderRepository.findByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                adapter.updateStatus(
                    storeId,
                    UUID.randomUUID(),
                    order.getId(),
                    new UpdateOrderStatusCommand(OrderStatus.COMPLETED, "invalid transition"),
                    Instant.now()))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

    verify(salesOrderRepository).findByIdAndStore_Id(order.getId(), storeId);
    verify(salesOrderRepository, never()).saveAndFlush(any());
    verifyNoInteractions(orderStatusHistoryRepository);
  }

  @Test
  void shouldCancelOrderWithSuccessAndTrimReason() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var canceledAt = Instant.parse("2026-03-22T19:10:00Z");
    var order = order(storeId, FulfillmentType.PICKUP);

    when(salesOrderRepository.findByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    var response =
        adapter.cancel(
            storeId,
            actorUserId,
            order.getId(),
            new CancelOrderCommand("  customer request  "),
            canceledAt);

    assertThat(response.id()).isEqualTo(order.getId());
    assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    assertThat(response.canceledAt()).isEqualTo(canceledAt);
    assertThat(response.reason()).isEqualTo("customer request");
    verify(salesOrderRepository).saveAndFlush(order);
    verify(orderStatusHistoryRepository).saveAndFlush(any(OrderStatusHistory.class));
  }

  @Test
  void shouldRejectNullCancelReason() {
    assertThatThrownBy(
            () ->
                adapter.cancel(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new CancelOrderCommand(null),
                    Instant.now()))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getDetails())
                  .singleElement()
                  .satisfies(detail -> assertThat(detail.field()).isEqualTo("reason"));
            });

    verifyNoInteractions(salesOrderRepository, orderStatusHistoryRepository);
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
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getDetails())
                  .singleElement()
                  .satisfies(detail -> assertThat(detail.field()).isEqualTo("reason"));
            });

    verifyNoInteractions(salesOrderRepository, orderStatusHistoryRepository);
  }

  @Test
  void shouldThrowWhenOrderIsMissingOnCancel() {
    var storeId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(salesOrderRepository.findByIdAndStore_Id(orderId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                adapter.cancel(
                    storeId,
                    UUID.randomUUID(),
                    orderId,
                    new CancelOrderCommand("customer request"),
                    Instant.now()))
        .isInstanceOf(OrderNotFoundException.class);

    verify(salesOrderRepository).findByIdAndStore_Id(orderId, storeId);
    verify(salesOrderRepository, never()).saveAndFlush(any());
    verifyNoInteractions(orderStatusHistoryRepository);
  }

  @Test
  void shouldConvertInvalidTransitionOnCancel() {
    var storeId = UUID.randomUUID();
    var order = order(storeId, FulfillmentType.PICKUP);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);
    order.changeStatus(OrderStatus.COMPLETED);

    when(salesOrderRepository.findByIdAndStore_Id(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                adapter.cancel(
                    storeId,
                    UUID.randomUUID(),
                    order.getId(),
                    new CancelOrderCommand("customer request"),
                    Instant.now()))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

    verify(salesOrderRepository).findByIdAndStore_Id(order.getId(), storeId);
    verify(salesOrderRepository, never()).saveAndFlush(any());
    verifyNoInteractions(orderStatusHistoryRepository);
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
    var deliveryFee =
        fulfillmentType == FulfillmentType.DELIVERY ? new BigDecimal("6.50") : BigDecimal.ZERO;
    var totalAmount =
        fulfillmentType == FulfillmentType.DELIVERY
            ? new BigDecimal("56.50")
            : new BigDecimal("50.00");

    return SalesOrder.create(
        UUID.randomUUID(),
        store,
        customer,
        fulfillmentType,
        PaymentMethod.PIX,
        new BigDecimal("50.00"),
        deliveryFee,
        totalAmount,
        null,
        null);
  }
}
