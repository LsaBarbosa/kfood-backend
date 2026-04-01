package com.kfood.order.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfood.order.app.port.OrderWorkflowPort;
import com.kfood.order.domain.OrderStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CancelOrderUseCaseTest {

  private final OrderWorkflowPort orderWorkflowPort = mock(OrderWorkflowPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-22T19:10:00Z"), ZoneOffset.UTC);
  private final CancelOrderUseCase useCase =
      new CancelOrderUseCase(
          orderWorkflowPort, currentTenantProvider, currentAuthenticatedUserProvider, clock);

  @Test
  void shouldCancelOrderWithReasonAndPersistAudit() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var command = new CancelOrderCommand("  Customer gave up on the order  ");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(orderWorkflowPort.cancel(
            eq(storeId),
            eq(actorUserId),
            eq(orderId),
            eq(new CancelOrderCommand("Customer gave up on the order")),
            eq(Instant.parse("2026-03-22T19:10:00Z"))))
        .thenReturn(
            new CancelOrderOutput(
                orderId,
                OrderStatus.CANCELED,
                Instant.parse("2026-03-22T19:10:00Z"),
                "Customer gave up on the order"));

    var response = useCase.execute(orderId, command);

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    assertThat(response.canceledAt()).isEqualTo(Instant.parse("2026-03-22T19:10:00Z"));
    assertThat(response.reason()).isEqualTo("Customer gave up on the order");

    verify(orderWorkflowPort)
        .cancel(
            storeId,
            actorUserId,
            orderId,
            new CancelOrderCommand("Customer gave up on the order"),
            Instant.parse("2026-03-22T19:10:00Z"));
  }

  @Test
  void shouldRejectCancellationWithoutReason() {
    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), new CancelOrderCommand("   ")))
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

    verifyNoInteractions(orderWorkflowPort);
  }

  @Test
  void shouldRejectCancellationOfCompletedOrder() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(orderWorkflowPort.cancel(any(), any(), any(), any(), any()))
        .thenThrow(
            new BusinessException(
                ErrorCode.ORDER_STATUS_TRANSITION_INVALID,
                "Invalid order status transition from COMPLETED to CANCELED",
                HttpStatus.CONFLICT));

    assertThatThrownBy(
            () -> useCase.execute(orderId, new CancelOrderCommand("Customer called too late")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
              assertThat(businessException.getStatus().value()).isEqualTo(409);
            })
        .hasMessage("Invalid order status transition from COMPLETED to CANCELED");
  }

  @Test
  void shouldReturnNotFoundWhenOrderDoesNotBelongToAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(orderWorkflowPort.cancel(any(), any(), any(), any(), any()))
        .thenThrow(new OrderNotFoundException(orderId));

    assertThatThrownBy(() -> useCase.execute(orderId, new CancelOrderCommand("Customer request")))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for id: " + orderId);
  }

  @Test
  void shouldRejectNullArguments() {
    assertThatThrownBy(() -> useCase.execute(null, new CancelOrderCommand("Customer request")))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("orderId must not be null");
    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
  }
}
