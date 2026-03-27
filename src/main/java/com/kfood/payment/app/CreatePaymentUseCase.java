package com.kfood.payment.app;

import com.kfood.order.app.OrderNotFoundException;
import com.kfood.order.infra.persistence.SalesOrderRepository;
import com.kfood.payment.infra.persistence.Payment;
import com.kfood.payment.infra.persistence.PaymentRepository;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({SalesOrderRepository.class, PaymentRepository.class})
public class CreatePaymentUseCase {

  private final SalesOrderRepository salesOrderRepository;
  private final PaymentRepository paymentRepository;

  public CreatePaymentUseCase(
      SalesOrderRepository salesOrderRepository, PaymentRepository paymentRepository) {
    this.salesOrderRepository = salesOrderRepository;
    this.paymentRepository = paymentRepository;
  }

  @Transactional
  public PaymentOutput execute(CreatePaymentCommand command) {
    var order =
        salesOrderRepository
            .findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

    var saved =
        paymentRepository.save(
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
