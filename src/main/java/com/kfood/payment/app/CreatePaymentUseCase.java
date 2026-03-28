package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.infra.persistence.Payment;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({PaymentOrderLookupPort.class, PaymentPersistencePort.class})
public class CreatePaymentUseCase {

  private final PaymentOrderLookupPort paymentOrderLookupPort;
  private final PaymentPersistencePort paymentPersistencePort;

  public CreatePaymentUseCase(
      PaymentOrderLookupPort paymentOrderLookupPort,
      PaymentPersistencePort paymentPersistencePort) {
    this.paymentOrderLookupPort = paymentOrderLookupPort;
    this.paymentPersistencePort = paymentPersistencePort;
  }

  @Transactional
  public PaymentOutput execute(CreatePaymentCommand command) {
    var order =
        paymentOrderLookupPort
            .findOrderById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    var saved =
        paymentPersistencePort.savePayment(
            Payment.createPending(
                UUID.randomUUID(), order, command.paymentMethod(), command.amount()));

    return new PaymentOutput(
        saved.getId(),
        saved.getOrder().getId(),
        saved.getPaymentMethod(),
        saved.getStatus(),
        saved.getAmount(),
        saved.getCreatedAt());
  }
}
