package com.kfood.payment.app;

import com.kfood.payment.infra.persistence.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({PaymentRepository.class, Clock.class})
public class UpdatePaymentStatusUseCase {

  private final PaymentRepository paymentRepository;
  private final Clock clock;

  public UpdatePaymentStatusUseCase(PaymentRepository paymentRepository, Clock clock) {
    this.paymentRepository = paymentRepository;
    this.clock = clock;
  }

  @Transactional
  public UpdatePaymentStatusOutput execute(UpdatePaymentStatusCommand command) {
    var payment =
        paymentRepository
            .findDetailedById(command.paymentId())
            .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

    payment.changeStatus(command.targetStatus(), Instant.now(clock));
    var snapshot = PaymentStatusSnapshotMapper.from(payment.getStatus());
    payment.getOrder().markPaymentStatusSnapshot(snapshot);

    return new UpdatePaymentStatusOutput(
        payment.getId(), payment.getOrder().getId(), payment.getStatus(), snapshot);
  }
}
