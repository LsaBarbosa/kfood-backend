package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentEntityTest {

  @Test
  void shouldExposeNormalizedPaymentData() {
    var id = UUID.randomUUID();
    var confirmedAt = Instant.parse("2026-03-27T15:00:00Z");
    var expiresAt = Instant.parse("2026-03-27T15:30:00Z");
    var payment =
        new Payment(
            id,
            order(),
            PaymentMethod.PIX,
            "  pix-sandbox  ",
            "  tx-123  ",
            PaymentStatus.PENDING,
            new BigDecimal("57.5"),
            "  copia-e-cola  ",
            confirmedAt,
            expiresAt);

    assertThat(payment.getId()).isEqualTo(id);
    assertThat(payment.getOrder()).isNotNull();
    assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(payment.getProviderName()).isEqualTo("pix-sandbox");
    assertThat(payment.getProviderReference()).isEqualTo("tx-123");
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getAmount()).isEqualByComparingTo("57.50");
    assertThat(payment.getQrCodePayload()).isEqualTo("copia-e-cola");
    assertThat(payment.getConfirmedAt()).isEqualTo(confirmedAt);
    assertThat(payment.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(payment.getCreatedAt()).isNull();
    assertThat(payment.getUpdatedAt()).isNull();
  }

  @Test
  void shouldKeepOptionalProviderFieldsAsNullWhenBlank() {
    var payment =
        new Payment(
            UUID.randomUUID(),
            order(),
            PaymentMethod.CASH,
            "   ",
            null,
            PaymentStatus.CONFIRMED,
            new BigDecimal("10.00"),
            "",
            null,
            null);

    assertThat(payment.getProviderName()).isNull();
    assertThat(payment.getProviderReference()).isNull();
    assertThat(payment.getQrCodePayload()).isNull();
    assertThat(payment.getConfirmedAt()).isNull();
    assertThat(payment.getExpiresAt()).isNull();
  }

  @Test
  void shouldCreatePendingPaymentUsingFactoryMethod() {
    var payment =
        Payment.createPending(
            UUID.randomUUID(), order(), PaymentMethod.PIX, new BigDecimal("57.5"));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(payment.getAmount()).isEqualByComparingTo("57.50");
    assertThat(payment.getProviderName()).isNull();
    assertThat(payment.getProviderReference()).isNull();
    assertThat(payment.getQrCodePayload()).isNull();
    assertThat(payment.getConfirmedAt()).isNull();
    assertThat(payment.getExpiresAt()).isNull();
  }

  @Test
  void shouldCreatePendingPixPaymentUsingFactoryMethod() {
    var payment = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.5"));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(payment.getAmount()).isEqualByComparingTo("57.50");
    assertThat(payment.getProviderName()).isNull();
    assertThat(payment.getProviderReference()).isNull();
    assertThat(payment.getQrCodePayload()).isNull();
    assertThat(payment.getExpiresAt()).isNull();
  }

  @Test
  void shouldAttachPixChargeData() {
    var payment = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.50"));
    var expiresAt = OffsetDateTime.parse("2026-03-27T16:00:00Z");

    payment.attachPixChargeData("  mock  ", "  tx-123  ", "  copia-e-cola  ", expiresAt);

    assertThat(payment.getProviderName()).isEqualTo("mock");
    assertThat(payment.getProviderReference()).isEqualTo("tx-123");
    assertThat(payment.getQrCodePayload()).isEqualTo("copia-e-cola");
    assertThat(payment.getExpiresAt()).isEqualTo(expiresAt.toInstant());
  }

  @Test
  void shouldAllowPendingToConfirmedTransition() {
    var payment = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.50"));

    payment.changeStatus(PaymentStatus.CONFIRMED);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
  }

  @Test
  void shouldAllowPendingToFailedCanceledAndExpiredTransitions() {
    var failed = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.50"));
    failed.changeStatus(PaymentStatus.FAILED);

    var canceled = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.50"));
    canceled.changeStatus(PaymentStatus.CANCELED);

    var expired = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.50"));
    expired.changeStatus(PaymentStatus.EXPIRED);

    assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(expired.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
  }

  @Test
  void shouldRejectInvalidPaymentStatusRegression() {
    var payment = Payment.createPendingPix(UUID.randomUUID(), order(), new BigDecimal("57.50"));
    payment.changeStatus(PaymentStatus.CONFIRMED);

    assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.FAILED))
        .isInstanceOf(PaymentStatusTransitionException.class)
        .hasMessage("Invalid payment status transition from CONFIRMED to FAILED");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<Payment> constructor = Payment.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private static SalesOrder order() {
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
    return SalesOrder.create(
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
  }
}
