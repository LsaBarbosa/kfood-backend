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
import com.kfood.payment.infra.persistence.InvalidPaymentStatusTransitionException;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdatePaymentStatusUseCaseTest {

  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final UpdatePaymentStatusUseCase useCase =
      new UpdatePaymentStatusUseCase(paymentRepository);

  @Test
  void shouldReflectPendingStatusOnOrder() {
    var payment = pendingPayment();
    payment.getOrder().markPaymentStatusSnapshot(PaymentStatusSnapshot.FAILED);
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.PENDING));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getOrder().getPaymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldReflectPaidStatusOnOrder() {
    var payment = pendingPayment();
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.CONFIRMED));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(payment.getOrder().getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PAID);
    assertThat(payment.getConfirmedAt()).isNotNull();
  }

  @Test
  void shouldReflectFailedStatusOnOrder() {
    var payment = pendingPayment();
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    useCase.execute(new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.FAILED));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payment.getOrder().getPaymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.FAILED);
  }

  @Test
  void shouldBlockInvalidPaymentStatusUpdate() {
    var payment = pendingPayment();
    payment.markConfirmed(java.time.OffsetDateTime.parse("2026-03-22T12:30:00Z"));
    payment.getOrder().markPaymentStatusSnapshot(PaymentStatusSnapshot.PAID);
    when(paymentRepository.findDetailedById(payment.getId())).thenReturn(Optional.of(payment));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new UpdatePaymentStatusCommand(payment.getId(), PaymentStatus.FAILED)))
        .isInstanceOf(InvalidPaymentStatusTransitionException.class)
        .hasMessage("Invalid payment status transition from CONFIRMED to FAILED");
  }

  @Test
  void shouldFailWhenPaymentDoesNotExist() {
    var paymentId = UUID.randomUUID();
    when(paymentRepository.findDetailedById(paymentId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.execute(new UpdatePaymentStatusCommand(paymentId, PaymentStatus.CONFIRMED)))
        .isInstanceOf(PaymentNotFoundException.class)
        .hasMessage("Payment not found for id: " + paymentId);
  }

  private Payment pendingPayment() {
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            mock(Store.class),
            mock(Customer.class),
            FulfillmentType.DELIVERY,
            PaymentMethod.PIX,
            new BigDecimal("42.00"),
            new BigDecimal("8.00"),
            new BigDecimal("50.00"),
            null,
            null);
    return Payment.create(UUID.randomUUID(), order, PaymentMethod.PIX, null, null, null);
  }
}
