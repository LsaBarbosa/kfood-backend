package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.Payment;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreatePaymentUseCaseTest {

  private final PaymentOrderLookupPort paymentOrderLookupPort = mock(PaymentOrderLookupPort.class);
  private final PaymentPersistencePort paymentPersistencePort = mock(PaymentPersistencePort.class);
  private final CreatePaymentUseCase useCase =
      new CreatePaymentUseCase(paymentOrderLookupPort, paymentPersistencePort);

  @Test
  void shouldCreatePendingPaymentForExistingOrder() {
    var order = order();
    var command =
        new CreatePaymentCommand(order.getId(), PaymentMethod.PIX, new BigDecimal("57.50"));
    var savedPayment =
        Payment.createPending(UUID.randomUUID(), order, PaymentMethod.PIX, new BigDecimal("57.50"));

    when(paymentOrderLookupPort.findOrderById(order.getId())).thenReturn(Optional.of(order));
    when(paymentPersistencePort.savePayment(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = useCase.execute(command);

    assertThat(result.id()).isNotNull();
    assertThat(result.orderId()).isEqualTo(order.getId());
    assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.amount()).isEqualByComparingTo("57.50");
    assertThat(result.createdAt()).isNull();
  }

  @Test
  void shouldFailWhenOrderDoesNotExist() {
    var orderId = UUID.randomUUID();
    var command = new CreatePaymentCommand(orderId, PaymentMethod.CASH, new BigDecimal("10.00"));

    when(paymentOrderLookupPort.findOrderById(orderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldPersistInitialPendingStatusAmountAndPaymentMethod() {
    var order = order();
    var command = new CreatePaymentCommand(order.getId(), PaymentMethod.CASH, new BigDecimal("10"));

    when(paymentOrderLookupPort.findOrderById(order.getId())).thenReturn(Optional.of(order));
    when(paymentPersistencePort.savePayment(any(Payment.class)))
        .thenAnswer(
            invocation -> {
              Payment payment = invocation.getArgument(0);
              return new Payment(
                  payment.getId(),
                  payment.getOrder(),
                  payment.getPaymentMethod(),
                  payment.getProviderName(),
                  payment.getProviderReference(),
                  payment.getStatus(),
                  payment.getAmount(),
                  payment.getQrCodePayload(),
                  payment.getConfirmedAt(),
                  payment.getExpiresAt());
            });

    var result = useCase.execute(command);

    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(result.amount()).isEqualByComparingTo("10.00");
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
