package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import com.kfood.payment.app.port.PaymentPersistencePort;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({PaymentOrderLookupPort.class, PaymentPersistencePort.class})
public class RegisterCashPaymentUseCase {

  private final PaymentOrderLookupPort paymentOrderLookupPort;
  private final PaymentPersistencePort paymentPersistencePort;

  public RegisterCashPaymentUseCase(
      PaymentOrderLookupPort paymentOrderLookupPort,
      PaymentPersistencePort paymentPersistencePort) {
    this.paymentOrderLookupPort = paymentOrderLookupPort;
    this.paymentPersistencePort = paymentPersistencePort;
  }

  @Transactional
  public PaymentOutput execute(RegisterCashPaymentCommand command) {
    var order =
        paymentOrderLookupPort
            .findOrderById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    if (!order.isCashPaymentEnabled()) {
      throw new CashPaymentNotEnabledException(order.getStoreId());
    }

    order.markPaymentMethodSnapshot(PaymentMethod.CASH);
    order.markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(PaymentStatus.PENDING));

    var saved =
        paymentPersistencePort.savePendingPayment(
            UUID.randomUUID(), order, PaymentMethod.CASH, order.getTotalAmount());

    return new PaymentOutput(
        saved.getId(),
        saved.getOrder().getId(),
        saved.getPaymentMethod(),
        saved.getStatus(),
        saved.getAmount(),
        saved.getCreatedAt());
  }
}
