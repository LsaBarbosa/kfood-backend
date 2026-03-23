package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.infra.persistence.PaymentRepository;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConfirmPaymentWebhookProcessor implements PaymentWebhookProcessor {

  private final PaymentRepository paymentRepository;

  public ConfirmPaymentWebhookProcessor(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Override
  @Transactional
  public void process(
      PaymentWebhookEvent event, com.kfood.payment.api.PaymentWebhookRequest request) {
    Objects.requireNonNull(event, "event must not be null");
    Objects.requireNonNull(request, "request must not be null");

    if (!isPaymentConfirmedEvent(request.eventType())) {
      return;
    }

    var providerReference = requireProviderReference(request.providerReference());
    var payment =
        paymentRepository
            .findByProviderNameAndProviderReference(
                normalizeProvider(event.getProviderName()), providerReference)
            .orElseThrow(
                () ->
                    new PaymentWebhookPaymentNotFoundException(
                        event.getProviderName(), providerReference));

    validateConfirmationAllowed(payment.getStatus());

    if (payment.getStatus() != PaymentStatus.CONFIRMED) {
      payment.markConfirmed(resolveConfirmedAt(request.paidAt()));
      payment
          .getOrder()
          .markPaymentStatusSnapshot(
              OrderPaymentStatusMapper.fromPaymentStatus(payment.getStatus()));
    }

    event.attachToPayment(payment);
    paymentRepository.save(payment);
  }

  private boolean isPaymentConfirmedEvent(String eventType) {
    return eventType != null && "PAYMENT_CONFIRMED".equalsIgnoreCase(eventType.trim());
  }

  private String requireProviderReference(String providerReference) {
    if (providerReference == null || providerReference.isBlank()) {
      throw new PaymentWebhookPaymentNotFoundException(null, null);
    }
    return providerReference.trim();
  }

  private String normalizeProvider(String providerName) {
    return providerName.trim().toLowerCase(Locale.ROOT);
  }

  private void validateConfirmationAllowed(PaymentStatus status) {
    if (status == PaymentStatus.CONFIRMED || status == PaymentStatus.PENDING) {
      return;
    }

    throw new PaymentConfirmationConflictException(status);
  }

  private OffsetDateTime resolveConfirmedAt(String paidAt) {
    if (paidAt == null || paidAt.isBlank()) {
      return OffsetDateTime.now();
    }

    return OffsetDateTime.parse(paidAt.trim());
  }
}
