package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdatePaymentStatusUseCaseTest {

  private final PaymentPersistencePort paymentPersistencePort = mock(PaymentPersistencePort.class);
  private final CurrentTenantProvider currentTenantProvider = mock(CurrentTenantProvider.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-27T18:00:00Z"), ZoneOffset.UTC);
  private final UpdatePaymentStatusUseCase useCase =
      new UpdatePaymentStatusUseCase(paymentPersistencePort, currentTenantProvider, clock);

  @Test
  void shouldReflectPendingPaymentAsPendingOrderSnapshot() {
    var payment = payment(PaymentStatus.PENDING);
    var order = (SalesOrder) payment.getOrder();
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(payment.getOrder().getStoreId());
    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(
            payment.getId(), payment.getOrder().getStoreId()))
        .thenReturn(Optional.of(payment));

    var result =
        useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.PENDING));

    assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldReflectConfirmedPaymentAsPaidOrderSnapshot() {
    var payment = payment(PaymentStatus.PENDING);
    var order = (SalesOrder) payment.getOrder();
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(payment.getOrder().getStoreId());
    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(
            payment.getId(), payment.getOrder().getStoreId()))
        .thenReturn(Optional.of(payment));

    var result =
        useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.CONFIRMED));

    assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(result.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
    assertThat(payment.getConfirmedAt()).isEqualTo(Instant.parse("2026-03-27T18:00:00Z"));
  }

  @Test
  void shouldReflectFailedPaymentAsFailedOrderSnapshot() {
    var payment = payment(PaymentStatus.PENDING);
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(payment.getOrder().getStoreId());
    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(
            payment.getId(), payment.getOrder().getStoreId()))
        .thenReturn(Optional.of(payment));

    var result =
        useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.FAILED));

    assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(result.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.FAILED);
    assertThat(payment.getConfirmedAt()).isNull();
  }

  @Test
  void shouldReflectCanceledAndExpiredPaymentAsFailedOrderSnapshot() {
    var canceled = payment(PaymentStatus.PENDING);
    var expired = payment(PaymentStatus.PENDING);
    when(currentTenantProvider.getRequiredStoreId())
        .thenReturn(canceled.getOrder().getStoreId(), expired.getOrder().getStoreId());
    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(
            canceled.getId(), canceled.getOrder().getStoreId()))
        .thenReturn(Optional.of(canceled));

    var canceledResult =
        useCase.execute(new UpdatePaymentStatusCommand(canceled.getId(), PaymentStatus.CANCELED));

    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(
            expired.getId(), expired.getOrder().getStoreId()))
        .thenReturn(Optional.of(expired));

    var expiredResult =
        useCase.execute(new UpdatePaymentStatusCommand(expired.getId(), PaymentStatus.EXPIRED));

    assertThat(canceledResult.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.FAILED);
    assertThat(expiredResult.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.FAILED);
  }

  @Test
  void shouldRejectInvalidPaymentStatusTransition() {
    var payment = payment(PaymentStatus.PENDING);
    payment.changeStatus(PaymentStatus.CONFIRMED);
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(payment.getOrder().getStoreId());
    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(
            payment.getId(), payment.getOrder().getStoreId()))
        .thenReturn(Optional.of(payment));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.FAILED)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var ex = (BusinessException) throwable;
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_STATUS_TRANSITION_INVALID);
              assertThat(ex)
                  .hasMessage("Invalid payment status transition from CONFIRMED to FAILED");
            });
  }

  @Test
  void shouldFailWhenPaymentDoesNotExist() {
    var paymentId = UUID.randomUUID();
    var storeId = UUID.randomUUID();
    when(currentTenantProvider.getRequiredStoreId()).thenReturn(storeId);
    when(paymentPersistencePort.findPaymentWithOrderByIdAndStoreId(paymentId, storeId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(new UpdatePaymentStatusCommand(paymentId, PaymentStatus.CONFIRMED)))
        .isInstanceOf(PaymentNotFoundException.class);
  }

  private static Payment payment(PaymentStatus status) {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var customer =
        new Customer(UUID.randomUUID(), store, "Maria Silva", "21999990000", "maria@email.com");
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            store,
            customer,
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("50.00"),
            new BigDecimal("7.50"),
            new BigDecimal("57.50"),
            null,
            null);
    var payment = Payment.createPendingPix(UUID.randomUUID(), order, new BigDecimal("57.50"));
    if (status != PaymentStatus.PENDING) {
      payment.changeStatus(status);
      order.markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(payment.getStatus()));
    }
    return payment;
  }
}
