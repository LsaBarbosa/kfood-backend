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
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.payment.infra.persistence.Payment;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterCashPaymentUseCaseTest {

  private final PaymentOrderLookupPort paymentOrderLookupPort = mock(PaymentOrderLookupPort.class);
  private final PaymentPersistencePort paymentPersistencePort = mock(PaymentPersistencePort.class);
  private final RegisterCashPaymentUseCase useCase =
      new RegisterCashPaymentUseCase(paymentOrderLookupPort, paymentPersistencePort);

  @Test
  void shouldRegisterCashPaymentWhenStoreAllowsIt() {
    var order = order(true);

    when(paymentOrderLookupPort.findOrderById(order.getId())).thenReturn(Optional.of(order));
    when(paymentPersistencePort.savePayment(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = useCase.execute(new RegisterCashPaymentCommand(order.getId()));

    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.CASH);
    assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
    assertThat(result.orderId()).isEqualTo(order.getId());
    assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.amount()).isEqualByComparingTo(order.getTotalAmount());
  }

  @Test
  void shouldFailWhenStoreDoesNotAcceptCash() {
    var order = order(false);

    when(paymentOrderLookupPort.findOrderById(order.getId())).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> useCase.execute(new RegisterCashPaymentCommand(order.getId())))
        .isInstanceOf(CashPaymentNotEnabledException.class);

    assertThat(order.getPaymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(order.getPaymentStatusSnapshot()).isEqualTo(PaymentStatusSnapshot.PENDING);
  }

  @Test
  void shouldFailWhenOrderDoesNotExist() {
    var orderId = UUID.randomUUID();

    when(paymentOrderLookupPort.findOrderById(orderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(new RegisterCashPaymentCommand(orderId)))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void shouldCreatePendingPaymentLinkedToOrderWithRealTotalAmount() {
    var order = order(true);

    when(paymentOrderLookupPort.findOrderById(order.getId())).thenReturn(Optional.of(order));
    when(paymentPersistencePort.savePayment(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = useCase.execute(new RegisterCashPaymentCommand(order.getId()));

    assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(result.amount()).isEqualByComparingTo("57.50");
    assertThat(result.orderId()).isEqualTo(order.getId());
  }

  private static SalesOrder order(boolean cashEnabled) {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    if (cashEnabled) {
      store.enableCashPayment();
    }
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
