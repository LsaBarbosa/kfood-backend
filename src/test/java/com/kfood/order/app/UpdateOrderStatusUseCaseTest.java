package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.order.infra.persistence.OrderStatusHistory;
import com.kfood.order.infra.persistence.OrderStatusHistoryRepository;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateOrderStatusUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final OrderStatusHistoryRepository orderStatusHistoryRepository =
      mock(OrderStatusHistoryRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-22T18:40:00Z"), ZoneOffset.UTC);
  private final UpdateOrderStatusUseCase useCase =
      new UpdateOrderStatusUseCase(
          salesOrderRepository,
          orderStatusHistoryRepository,
          currentTenantProvider,
          currentAuthenticatedUserProvider,
          clock);

  @Test
  void shouldPersistValidStatusUpdateAndAuditHistory() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var order = order(storeId, FulfillmentType.DELIVERY);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));
    when(salesOrderRepository.saveAndFlush(order)).thenReturn(order);
    when(orderStatusHistoryRepository.saveAndFlush(any(OrderStatusHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        useCase.execute(
            order.getId(),
            new UpdateOrderStatusCommand(OrderStatus.PREPARING, "  Order entered preparation  "));

    assertThat(response.id()).isEqualTo(order.getId());
    assertThat(response.previousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(response.newStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(response.changedBy()).isEqualTo(actorUserId);
    assertThat(response.changedAt()).isEqualTo(Instant.parse("2026-03-22T18:40:00Z"));
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

    var historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
    verify(orderStatusHistoryRepository).saveAndFlush(historyCaptor.capture());

    var history = historyCaptor.getValue();
    assertThat(history.getStoreId()).isEqualTo(storeId);
    assertThat(history.getOrderId()).isEqualTo(order.getId());
    assertThat(history.getPreviousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(history.getNewStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(history.getActorUserId()).isEqualTo(actorUserId);
    assertThat(history.getChangedAt()).isEqualTo(Instant.parse("2026-03-22T18:40:00Z"));
    assertThat(history.getReason()).isEqualTo("Order entered preparation");
  }

  @Test
  void shouldFailWhenTransitionIsInvalid() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var order = order(storeId, FulfillmentType.DELIVERY);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    order.getId(), new UpdateOrderStatusCommand(OrderStatus.READY, null)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
              assertThat(businessException.getStatus().value()).isEqualTo(409);
            })
        .hasMessage("Invalid order status transition from NEW to READY");

    assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
    verifyNoInteractions(orderStatusHistoryRepository);
  }

  @Test
  void shouldRejectCanceledOnStatusEndpoint() {
    assertThatThrownBy(
            () ->
                useCase.execute(
                    UUID.randomUUID(),
                    new UpdateOrderStatusCommand(OrderStatus.CANCELED, "Customer canceled")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(businessException.getStatus().value()).isEqualTo(400);
            })
        .hasMessage("Use the cancel endpoint to move an order to CANCELED.");

    verifyNoInteractions(salesOrderRepository);
    verifyNoInteractions(orderStatusHistoryRepository);
  }

  @Test
  void shouldReturnNotFoundWhenOrderDoesNotBelongToAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(salesOrderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(orderId, new UpdateOrderStatusCommand(OrderStatus.PREPARING, null)))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for id: " + orderId);
  }

  @Test
  void shouldRejectNullArguments() {
    assertThatThrownBy(
            () -> useCase.execute(null, new UpdateOrderStatusCommand(OrderStatus.NEW, null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("orderId must not be null");
    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThatThrownBy(
            () -> useCase.execute(UUID.randomUUID(), new UpdateOrderStatusCommand(null, null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("newStatus must not be null");
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
