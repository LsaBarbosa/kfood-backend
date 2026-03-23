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
import com.kfood.order.api.CancelOrderRequest;
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

class CancelOrderUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final OrderStatusHistoryRepository orderStatusHistoryRepository =
      mock(OrderStatusHistoryRepository.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-22T19:10:00Z"), ZoneOffset.UTC);
  private final CancelOrderUseCase useCase =
      new CancelOrderUseCase(
          salesOrderRepository,
          orderStatusHistoryRepository,
          currentTenantProvider,
          currentAuthenticatedUserProvider,
          clock);

  @Test
  void shouldCancelOrderWithReasonAndPersistAudit() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var order = newOrder(storeId, FulfillmentType.DELIVERY);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));
    when(salesOrderRepository.saveAndFlush(order)).thenReturn(order);
    when(orderStatusHistoryRepository.saveAndFlush(any(OrderStatusHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        useCase.execute(order.getId(), new CancelOrderRequest("  Customer gave up on the order  "));

    assertThat(response.id()).isEqualTo(order.getId());
    assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    assertThat(response.canceledAt()).isEqualTo(Instant.parse("2026-03-22T19:10:00Z"));
    assertThat(response.reason()).isEqualTo("Customer gave up on the order");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);

    var historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
    verify(orderStatusHistoryRepository).saveAndFlush(historyCaptor.capture());

    var history = historyCaptor.getValue();
    assertThat(history.getStoreId()).isEqualTo(storeId);
    assertThat(history.getOrderId()).isEqualTo(order.getId());
    assertThat(history.getPreviousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(history.getNewStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(history.getActorUserId()).isEqualTo(actorUserId);
    assertThat(history.getChangedAt()).isEqualTo(Instant.parse("2026-03-22T19:10:00Z"));
    assertThat(history.getReason()).isEqualTo("Customer gave up on the order");
  }

  @Test
  void shouldRejectCancellationWithoutReason() {
    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), new CancelOrderRequest("   ")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
              assertThat(businessException.getStatus().value()).isEqualTo(400);
              assertThat(businessException.getDetails()).hasSize(1);
              assertThat(businessException.getDetails().getFirst().field()).isEqualTo("reason");
            })
        .hasMessage("Cancellation reason must not be blank.");

    verifyNoInteractions(salesOrderRepository);
    verifyNoInteractions(orderStatusHistoryRepository);
  }

  @Test
  void shouldRejectCancellationOfCompletedOrder() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var order = newOrder(storeId, FulfillmentType.PICKUP);
    order.changeStatus(OrderStatus.PREPARING);
    order.changeStatus(OrderStatus.READY);
    order.changeStatus(OrderStatus.COMPLETED);

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(salesOrderRepository.findByIdAndStoreId(order.getId(), storeId))
        .thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                useCase.execute(order.getId(), new CancelOrderRequest("Customer called too late")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
              assertThat(businessException.getStatus().value()).isEqualTo(409);
            })
        .hasMessage("Invalid order status transition from COMPLETED to CANCELED");

    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
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

    assertThatThrownBy(() -> useCase.execute(orderId, new CancelOrderRequest("Customer request")))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for id: " + orderId);
  }

  @Test
  void shouldRejectNullArguments() {
    assertThatThrownBy(() -> useCase.execute(null, new CancelOrderRequest("Customer request")))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("orderId must not be null");
    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("request must not be null");
  }

  @Test
  void shouldRejectNullCancellationReason() {
    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), new CancelOrderRequest(null)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Cancellation reason must not be blank.");
  }

  private SalesOrder newOrder(UUID storeId, FulfillmentType fulfillmentType) {
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
