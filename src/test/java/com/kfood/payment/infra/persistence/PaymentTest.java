package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  void shouldCreatePaymentLinkedToOrderWithPendingInitialStatus() {
    var payment =
        Payment.create(
            UUID.randomUUID(),
            order(new BigDecimal("48.00")),
            PaymentMethod.PIX,
            " mock-psp ",
            " pay_123 ",
            " pix-copy-paste ");

    assertThat(payment.getOrder()).isNotNull();
    assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getAmount()).isEqualByComparingTo("48.00");
    assertThat(payment.getProviderName()).isEqualTo("mock-psp");
    assertThat(payment.getProviderReference()).isEqualTo("pay_123");
    assertThat(payment.getQrCodePayload()).isEqualTo("pix-copy-paste");
    assertThat(payment.getConfirmedAt()).isNull();
    assertThat(payment.getExpiresAt()).isNull();
  }

  @Test
  void shouldAttachPixChargeData() {
    var payment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("48.00")), PaymentMethod.PIX, null, null, null);
    var expiresAt = OffsetDateTime.parse("2026-03-22T12:45:00Z");

    payment.attachPixCharge(" mock-psp ", " pix_123 ", " pix-copy-paste ", expiresAt);

    assertThat(payment.getProviderName()).isEqualTo("mock-psp");
    assertThat(payment.getProviderReference()).isEqualTo("pix_123");
    assertThat(payment.getQrCodePayload()).isEqualTo("pix-copy-paste");
    assertThat(payment.getExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void shouldUpdatePaymentStatusLifecycle() {
    var confirmedPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    var confirmedAt = OffsetDateTime.parse("2026-03-22T12:30:00Z");

    confirmedPayment.markConfirmed(confirmedAt);
    assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(confirmedPayment.getConfirmedAt()).isEqualTo(confirmedAt);

    var failedPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    failedPayment.markFailed();
    assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(failedPayment.getConfirmedAt()).isNull();

    var canceledPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    canceledPayment.markCanceled();
    assertThat(canceledPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);

    var expiredPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    expiredPayment.markExpired();
    assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
  }

  @Test
  void shouldRejectInvalidPaymentStatusTransition() {
    var payment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    payment.markConfirmed(OffsetDateTime.parse("2026-03-22T12:30:00Z"));

    assertThatThrownBy(payment::markFailed)
        .isInstanceOf(InvalidPaymentStatusTransitionException.class)
        .hasMessage("Invalid payment status transition from CONFIRMED to FAILED");
  }

  @Test
  void shouldRejectInvalidPaymentArguments() {
    assertThatThrownBy(
            () ->
                Payment.create(
                    null, order(new BigDecimal("48.00")), PaymentMethod.PIX, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("id must not be null");
    assertThatThrownBy(
            () -> Payment.create(UUID.randomUUID(), null, PaymentMethod.PIX, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("order must not be null");
    assertThatThrownBy(
            () ->
                Payment.create(
                    UUID.randomUUID(), order(new BigDecimal("48.00")), null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("paymentMethod must not be null");
    assertThatThrownBy(
            () ->
                Payment.create(
                    UUID.randomUUID(),
                    orderWithNullTotalAmount(),
                    PaymentMethod.PIX,
                    null,
                    null,
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("amount must not be null");
    assertThatCode(
            () ->
                Payment.create(
                    UUID.randomUUID(),
                    order(new BigDecimal("48.00")),
                    PaymentMethod.PIX,
                    " ",
                    " ",
                    " "))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectInvalidLifecycleStateAndNullConfirmation() throws Exception {
    var payment =
        Payment.create(
            UUID.randomUUID(),
            order(new BigDecimal("48.00")),
            PaymentMethod.CASH,
            null,
            null,
            null);

    assertThatThrownBy(() -> payment.markConfirmed(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("confirmedAt must not be null");
    assertThatThrownBy(
            () ->
                payment.attachPixCharge(
                    " ", "pix_123", "payload", OffsetDateTime.parse("2026-03-22T12:30:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerName must not be blank");

    setField(payment, "amount", new BigDecimal("-1.00"));

    assertThatThrownBy(() -> invokeValidateLifecycle(payment))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("amount must not be negative");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<Payment> constructor = Payment.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThat(constructor.newInstance()).isNotNull();
  }

  @Test
  void shouldIgnoreRepeatedTransitions() throws Exception {
    var confirmedPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    var confirmedAt = OffsetDateTime.parse("2026-03-22T12:30:00Z");
    confirmedPayment.markConfirmed(confirmedAt);
    confirmedPayment.markConfirmed(confirmedAt.plusMinutes(1));
    assertThat(confirmedPayment.getConfirmedAt()).isEqualTo(confirmedAt);

    var failedPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    failedPayment.markFailed();
    failedPayment.markFailed();
    assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

    var canceledPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    canceledPayment.markCanceled();
    canceledPayment.markCanceled();
    assertThat(canceledPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);

    var expiredPayment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);
    expiredPayment.markExpired();
    expiredPayment.markExpired();
    assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);

    invokeTransitionTo(expiredPayment, PaymentStatus.EXPIRED, null);
    assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
  }

  @Test
  void shouldExposePendingTransitionMatrixThroughPrivateGuard() throws Exception {
    var payment =
        Payment.create(
            UUID.randomUUID(), order(new BigDecimal("57.50")), PaymentMethod.PIX, null, null, null);

    assertThat(invokeCanTransitionTo(payment, PaymentStatus.CONFIRMED)).isTrue();
    assertThat(invokeCanTransitionTo(payment, PaymentStatus.FAILED)).isTrue();
    assertThat(invokeCanTransitionTo(payment, PaymentStatus.CANCELED)).isTrue();
    assertThat(invokeCanTransitionTo(payment, PaymentStatus.EXPIRED)).isTrue();
    assertThat(invokeCanTransitionTo(payment, PaymentStatus.PENDING)).isFalse();
  }

  private SalesOrder order(BigDecimal totalAmount) {
    var deliveryFee = new BigDecimal("8.00");
    return SalesOrder.create(
        UUID.randomUUID(),
        mock(Store.class),
        mock(Customer.class),
        FulfillmentType.DELIVERY,
        PaymentMethod.PIX,
        totalAmount.subtract(deliveryFee),
        deliveryFee,
        totalAmount,
        null,
        null);
  }

  private SalesOrder orderWithNullTotalAmount() {
    var order = mock(SalesOrder.class);
    org.mockito.Mockito.when(order.getTotalAmount()).thenReturn(null);
    return order;
  }

  private void invokeValidateLifecycle(Payment payment) throws Exception {
    Method method = Payment.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);
    method.invoke(payment);
  }

  private void setField(Payment payment, String fieldName, Object value) throws Exception {
    Field field = Payment.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(payment, value);
  }

  private void invokeTransitionTo(
      Payment payment, PaymentStatus nextStatus, OffsetDateTime confirmedAt) throws Exception {
    Method method =
        Payment.class.getDeclaredMethod("transitionTo", PaymentStatus.class, OffsetDateTime.class);
    method.setAccessible(true);
    method.invoke(payment, nextStatus, confirmedAt);
  }

  private boolean invokeCanTransitionTo(Payment payment, PaymentStatus nextStatus)
      throws Exception {
    Method method = Payment.class.getDeclaredMethod("canTransitionTo", PaymentStatus.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(payment, nextStatus);
  }
}
