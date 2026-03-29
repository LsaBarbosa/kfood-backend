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

class UpdateOrderStatusUseCaseTest {

  private final OrderWorkflowPort orderWorkflowPort = mock(OrderWorkflowPort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider =
      mock(CurrentAuthenticatedUserProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-22T18:40:00Z"), ZoneOffset.UTC);
  private final UpdateOrderStatusUseCase useCase =
      new UpdateOrderStatusUseCase(
          orderWorkflowPort, currentTenantProvider, currentAuthenticatedUserProvider, clock);

  @Test
  void shouldPersistValidStatusUpdateAndAuditHistory() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var command =
        new UpdateOrderStatusCommand(OrderStatus.PREPARING, "  Order entered preparation  ");

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(orderWorkflowPort.updateStatus(
            eq(storeId),
            eq(actorUserId),
            eq(orderId),
            eq(command),
            eq(Instant.parse("2026-03-22T18:40:00Z"))))
        .thenReturn(
            new UpdateOrderStatusOutput(
                orderId,
                OrderStatus.NEW,
                OrderStatus.PREPARING,
                Instant.parse("2026-03-22T18:40:00Z"),
                actorUserId));

    var response = useCase.execute(orderId, command);

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.previousStatus()).isEqualTo(OrderStatus.NEW);
    assertThat(response.newStatus()).isEqualTo(OrderStatus.PREPARING);
    assertThat(response.changedBy()).isEqualTo(actorUserId);
    assertThat(response.changedAt()).isEqualTo(Instant.parse("2026-03-22T18:40:00Z"));

    verify(orderWorkflowPort)
        .updateStatus(
            storeId, actorUserId, orderId, command, Instant.parse("2026-03-22T18:40:00Z"));
  }

  @Test
  void shouldFailWhenTransitionIsInvalid() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(orderWorkflowPort.updateStatus(any(), any(), any(), any(), any()))
        .thenThrow(
            new BusinessException(
                ErrorCode.ORDER_STATUS_TRANSITION_INVALID,
                "Invalid order status transition from NEW to READY",
                HttpStatus.CONFLICT));

    assertThatThrownBy(
            () -> useCase.execute(orderId, new UpdateOrderStatusCommand(OrderStatus.READY, null)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
              assertThat(businessException.getStatus().value()).isEqualTo(409);
            })
        .hasMessage("Invalid order status transition from NEW to READY");
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

    verifyNoInteractions(orderWorkflowPort);
  }

  @Test
  void shouldReturnNotFoundWhenOrderDoesNotBelongToAuthenticatedTenant() {
    var storeId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(currentAuthenticatedUserProvider.getRequiredUserId()).thenReturn(actorUserId);
    when(orderWorkflowPort.updateStatus(any(), any(), any(), any(), any()))
        .thenThrow(new OrderNotFoundException(orderId));

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
}
