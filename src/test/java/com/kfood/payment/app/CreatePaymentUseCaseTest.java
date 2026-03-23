package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreatePaymentUseCaseTest {

  private final SalesOrderRepository salesOrderRepository = mock(SalesOrderRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final CreatePaymentUseCase useCase =
      new CreatePaymentUseCase(salesOrderRepository, paymentRepository);

  @Test
  void shouldCreatePaymentForExistingOrder() {
    var order = order(new BigDecimal("57.50"));
    var command =
        new CreatePaymentCommand(order.getId(), PaymentMethod.PIX, "mock-psp", "pix_123", "qr");

    when(salesOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(paymentRepository.saveAndFlush(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Payment.class));

    var createdPayment = useCase.execute(command);

    assertThat(createdPayment.getOrder()).isEqualTo(order);
    assertThat(createdPayment.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
    assertThat(createdPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(createdPayment.getAmount()).isEqualByComparingTo("57.50");
    verify(paymentRepository).saveAndFlush(any(Payment.class));
  }

  @Test
  void shouldRejectInvalidCommandArguments() {
    assertThatThrownBy(() -> useCase.execute(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePaymentCommand(null, PaymentMethod.PIX, null, null, null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("orderId must not be null");
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new CreatePaymentCommand(UUID.randomUUID(), null, null, null, null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("paymentMethod must not be null");
  }

  @Test
  void shouldRejectPaymentCreationWhenOrderDoesNotExist() {
    var missingOrderId = UUID.randomUUID();
    var command = new CreatePaymentCommand(missingOrderId, PaymentMethod.CASH, null, null, null);

    when(salesOrderRepository.findById(missingOrderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found for id: " + missingOrderId);
  }

  private SalesOrder order(BigDecimal totalAmount) {
    return SalesOrder.create(
        UUID.randomUUID(),
        mock(Store.class),
        mock(Customer.class),
        FulfillmentType.DELIVERY,
        PaymentMethod.PIX,
        new BigDecimal("50.00"),
        new BigDecimal("7.50"),
        totalAmount,
        null,
        null);
  }
}
