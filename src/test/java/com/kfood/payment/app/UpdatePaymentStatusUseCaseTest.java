package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.payment.infra.persistence.PaymentStatusTransitionException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdatePaymentStatusUseCaseTest {

  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final UpdatePaymentStatusUseCase useCase =
      new UpdatePaymentStatusUseCase(paymentRepository);

  @Test
  void shouldReflectPendingPaymentAsPendingOrderSnapshot() {
    var payment = payment(PaymentStatus.PENDING);
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    var result =
        useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.PENDING));

    assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(payment.getOrder().getPaymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldReflectConfirmedPaymentAsPaidOrderSnapshot() {
    var payment = payment(PaymentStatus.PENDING);
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    var result =
        useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.CONFIRMED));

    assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(result.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
    assertThat(payment.getOrder().getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
  }

  @Test
  void shouldReflectFailedPaymentAsFailedOrderSnapshot() {
    var payment = payment(PaymentStatus.PENDING);
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    var result =
        useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.FAILED));

    assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(result.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.FAILED);
  }

  @Test
  void shouldReflectCanceledAndExpiredPaymentAsFailedOrderSnapshot() {
    var canceled = payment(PaymentStatus.PENDING);
    when(paymentRepository.findDetailedById(canceled.getId())).thenReturn(Optional.of(canceled));

    var canceledResult =
        useCase.execute(new UpdatePaymentStatusCommand(canceled.getId(), PaymentStatus.CANCELED));

    var expired = payment(PaymentStatus.PENDING);
    when(paymentRepository.findDetailedById(expired.getId())).thenReturn(Optional.of(expired));

    var expiredResult =
        useCase.execute(new UpdatePaymentStatusCommand(expired.getId(), PaymentStatus.EXPIRED));

    assertThat(canceledResult.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.FAILED);
    assertThat(expiredResult.orderPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.FAILED);
  }

  @Test
  void shouldRejectInvalidPaymentStatusTransition() {
    var payment = payment(PaymentStatus.PENDING);
    payment.changeStatus(PaymentStatus.CONFIRMED);
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.FAILED)))
        .isInstanceOf(PaymentStatusTransitionException.class)
        .hasMessage("Invalid payment status transition from CONFIRMED to FAILED");
  }

  @Test
  void shouldFailWhenPaymentDoesNotExist() {
    var paymentId = UUID.randomUUID();
    when(paymentRepository.findDetailedById(paymentId)).thenReturn(Optional.empty());

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
