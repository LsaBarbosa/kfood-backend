package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatusTransitionException;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({PaymentRepository.class, CurrentTenantProvider.class, Clock.class})
public class UpdatePaymentStatusUseCase {

  private final PaymentRepository paymentRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final Clock clock;

  public UpdatePaymentStatusUseCase(
      PaymentRepository paymentRepository, CurrentTenantProvider currentTenantProvider, Clock clock) {
    this.paymentRepository = paymentRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.clock = clock;
  }

  @Transactional
  public UpdatePaymentStatusOutput execute(UpdatePaymentStatusCommand command) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var payment =
        paymentRepository
            .findDetailedByIdAndOrder_Store_Id(command.paymentId(), storeId)
            .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

    try {
      payment.changeStatus(command.targetStatus(), Instant.now(clock));
    } catch (PaymentStatusTransitionException exception) {
      throw new BusinessException(
          ErrorCode.PAYMENT_STATUS_TRANSITION_INVALID, exception.getMessage(), HttpStatus.CONFLICT);
    }

    var snapshot = PaymentStatusSnapshotMapper.from(payment.getStatus());
    payment.getOrder().markPaymentStatusSnapshot(snapshot);

    return new UpdatePaymentStatusOutput(
        payment.getId(), payment.getOrder().getId(), payment.getStatus(), snapshot);
  }
}
