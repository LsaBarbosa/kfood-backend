package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterOrderPaymentServiceTest {

  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final RegisterOrderPaymentService service =
      new RegisterOrderPaymentService(paymentRepository);

  @Test
  void shouldCreatePaymentForCashOrder() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var order = order(store, PaymentMethod.CASH);

    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Payment.class));

    service.registerInitialPayment(order);

    var payment = org.mockito.ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).saveAndFlush(payment.capture());
    assertThat(payment.getValue().getOrder()).isEqualTo(order);
    assertThat(payment.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  void shouldIgnorePixOrder() {
    service.registerInitialPayment(order(enabledStore(), PaymentMethod.PIX));

    verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
  }

  @Test
  void shouldRejectCashWhenStoreDoesNotAcceptIt() {
    var store = enabledStore();
    store.disableCashPayment();

    assertThatThrownBy(() -> service.registerInitialPayment(order(store, PaymentMethod.CASH)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);

    verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
  }

  @Test
  void shouldRejectNullOrder() {
    assertThatThrownBy(() -> service.registerInitialPayment(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("order must not be null");
  }

  private Store enabledStore() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private SalesOrder order(Store store, PaymentMethod paymentMethod) {
    return SalesOrder.create(
        UUID.randomUUID(),
        store,
        mock(Customer.class),
        FulfillmentType.PICKUP,
        paymentMethod,
        new BigDecimal("50.00"),
        BigDecimal.ZERO,
        new BigDecimal("50.00"),
        null,
        null);
  }
}
