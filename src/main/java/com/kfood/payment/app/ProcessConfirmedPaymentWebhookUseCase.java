package com.kfood.payment.app;

import com.kfood.payment.app.port.PaymentRecord;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import com.kfood.payment.app.port.PaymentWebhookPaymentPort;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusTransitionException;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessConfirmedPaymentWebhookUseCase {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  private final PaymentWebhookPaymentPort paymentWebhookPaymentPort;
  private final Clock clock;

  public ProcessConfirmedPaymentWebhookUseCase(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort,
      PaymentWebhookPaymentPort paymentWebhookPaymentPort,
      Clock clock) {
    this.paymentWebhookEventPersistencePort = paymentWebhookEventPersistencePort;
    this.paymentWebhookPaymentPort = paymentWebhookPaymentPort;
    this.clock = clock;
  }

  @Transactional
  public PaymentWebhookEventRecord execute(
      PaymentWebhookEventRecord event, String providerReference) {
    return process(event, providerReference, false);
  }

  @Transactional
  public PaymentWebhookEventRecord executeOrThrow(
      PaymentWebhookEventRecord event, String providerReference) {
    return process(event, providerReference, true);
  }

  private PaymentWebhookEventRecord process(
      PaymentWebhookEventRecord event, String providerReference, boolean throwOnControlledFailure) {
    var processedAt = Instant.now(clock);
    var normalizedProviderReference = normalizeProviderReference(providerReference);
    if (normalizedProviderReference == null) {
      var failedEvent = paymentWebhookEventPersistencePort.markFailed(event.getId(), processedAt);
      if (throwOnControlledFailure) {
        throw new BusinessException(
            ErrorCode.VALIDATION_ERROR,
            "providerReference must not be blank for supported webhook event types.",
            HttpStatus.BAD_REQUEST);
      }
      return failedEvent;
    }

    var correlatedPayment =
        paymentWebhookPaymentPort.findByProviderNameAndProviderReference(
            event.getProviderName(), normalizedProviderReference);
    if (correlatedPayment.isEmpty()) {
      var failedEvent = paymentWebhookEventPersistencePort.markFailed(event.getId(), processedAt);
      if (throwOnControlledFailure) {
        throw new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Payment not found for providerReference.",
            HttpStatus.NOT_FOUND);
      }
      return failedEvent;
    }

    var payment = correlatedPayment.get();
    try {
      applyPaymentStatus(event.getEventType(), payment, processedAt);
    } catch (PaymentStatusTransitionException exception) {
      var failedEvent = paymentWebhookEventPersistencePort.markFailed(event.getId(), processedAt);
      if (throwOnControlledFailure) {
        throw new BusinessException(
            ErrorCode.PAYMENT_STATUS_TRANSITION_INVALID,
            exception.getMessage(),
            HttpStatus.CONFLICT);
      }
      return failedEvent;
    }

    return paymentWebhookEventPersistencePort.markProcessed(
        event.getId(), payment.getId(), processedAt);
  }

  private void applyPaymentStatus(String eventType, PaymentRecord payment, Instant processedAt) {
    payment.changeStatus(resolveTargetStatus(eventType), processedAt);
    payment
        .getOrder()
        .markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(payment.getStatus()));
  }

  private PaymentStatus resolveTargetStatus(String eventType) {
    return switch (eventType) {
      case "PAYMENT_PENDING" -> PaymentStatus.PENDING;
      case "PAYMENT_CONFIRMED" -> PaymentStatus.CONFIRMED;
      case "PAYMENT_FAILED" -> PaymentStatus.FAILED;
      case "PAYMENT_CANCELED" -> PaymentStatus.CANCELED;
      case "PAYMENT_EXPIRED" -> PaymentStatus.EXPIRED;
      default ->
          throw new IllegalArgumentException(
              "Unsupported payment webhook event type: " + eventType);
    };
  }

  private String normalizeProviderReference(String providerReference) {
    if (providerReference == null || providerReference.isBlank()) {
      return null;
    }
    return providerReference.trim();
  }
}
