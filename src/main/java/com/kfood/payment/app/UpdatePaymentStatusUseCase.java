package com.kfood.payment.app;

import com.kfood.payment.infra.persistence.PaymentRepository;
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
  public UpdatePaymentStatusOutput execute(UpdatePaymentStatusCommand command) {
    var payment =
        paymentRepository
            .findDetailedById(command.paymentId())
            .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

    payment.changeStatus(command.targetStatus());
    var snapshot = PaymentStatusSnapshotMapper.from(payment.getStatus());
    payment.getOrder().markPaymentStatusSnapshot(snapshot);

    return new UpdatePaymentStatusOutput(
        payment.getId(), payment.getOrder().getId(), payment.getStatus(), snapshot);
  }
}
