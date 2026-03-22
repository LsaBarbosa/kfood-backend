package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.InvalidPaymentStatusTransitionException;
import com.kfood.payment.infra.persistence.PaymentRepository;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(PaymentRepository.class)
public class UpdatePaymentStatusUseCase {

  private final PaymentRepository paymentRepository;

  public UpdatePaymentStatusUseCase(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Transactional
  public void execute(UpdatePaymentStatusCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.paymentId(), "paymentId must not be null");
    Objects.requireNonNull(command.newStatus(), "newStatus must not be null");

    var payment =
        paymentRepository
            .findDetailedById(command.paymentId())
            .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

    transitionPayment(payment, command.newStatus());
    payment
        .getOrder()
        .markPaymentStatusSnapshot(OrderPaymentStatusMapper.fromPaymentStatus(payment.getStatus()));
    paymentRepository.saveAndFlush(payment);
  }

  private void transitionPayment(
      com.kfood.payment.infra.persistence.Payment payment, PaymentStatus newStatus) {
    switch (newStatus) {
      case PENDING -> {
        if (payment.getStatus() != PaymentStatus.PENDING) {
          throw new InvalidPaymentStatusTransitionException(
              payment.getStatus(), PaymentStatus.PENDING);
        }
      }
      case CONFIRMED -> payment.markConfirmed(OffsetDateTime.now());
      case FAILED -> payment.markFailed();
      case CANCELED -> payment.markCanceled();
      case EXPIRED -> payment.markExpired();
    }
  }
}
