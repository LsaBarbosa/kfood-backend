package com.kfood.payment.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class PaymentWebhookPaymentNotFoundException extends BusinessException {

  public PaymentWebhookPaymentNotFoundException(String providerName, String providerReference) {
    super(
        ErrorCode.RESOURCE_NOT_FOUND,
        buildMessage(providerName, providerReference),
        HttpStatus.NOT_FOUND);
  }

  private static String buildMessage(String providerName, String providerReference) {
    if (providerName == null
        || providerName.isBlank()
        || providerReference == null
        || providerReference.isBlank()) {
      return "Payment not found for the informed provider and reference.";
    }

    return "Payment not found for provider "
        + providerName
        + " and reference "
        + providerReference
        + ".";
  }
}
