package com.kfood.payment.app;

import com.kfood.order.app.OrderPaymentRegistrar;
import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(PaymentRepository.class)
public class RegisterOrderPaymentService implements OrderPaymentRegistrar {

  private final PaymentRepository paymentRepository;

  public RegisterOrderPaymentService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Override
  @Transactional
  public void registerInitialPayment(SalesOrder order) {
    var validatedOrder = Objects.requireNonNull(order, "order must not be null");

    if (validatedOrder.getPaymentMethod() == PaymentMethod.PIX) {
      return;
    }

    if (validatedOrder.getPaymentMethod() == PaymentMethod.CASH) {
      if (!validatedOrder.getStore().acceptsCashPayments()) {
        throw new BusinessException(
            ErrorCode.VALIDATION_ERROR,
            "Store does not accept cash payments.",
            HttpStatus.BAD_REQUEST);
      }

      paymentRepository.saveAndFlush(
          Payment.create(UUID.randomUUID(), validatedOrder, PaymentMethod.CASH, null, null, null));
    }
  }
}
