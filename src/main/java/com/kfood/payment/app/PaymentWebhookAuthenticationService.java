package com.kfood.payment.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentWebhookAuthenticationService {

  private final List<PaymentWebhookAuthenticator> authenticators;

  public PaymentWebhookAuthenticationService(List<PaymentWebhookAuthenticator> authenticators) {
    this.authenticators = authenticators;
  }

  public boolean isValid(String provider, String token) {
    var authenticator =
        authenticators.stream().filter(candidate -> candidate.supports(provider)).findFirst();
    if (authenticator.isEmpty()) {
      return false;
    }
    try {
      authenticator.get().authenticate(token);
      return true;
    } catch (BusinessException exception) {
      return false;
    }
  }

  public void authenticate(String provider, String token) {
    if (!isValid(provider, token)) {
      throw invalidWebhookToken();
    }
  }

  private BusinessException invalidWebhookToken() {
    return new BusinessException(
        ErrorCode.WEBHOOK_SIGNATURE_INVALID, "Invalid webhook token.", HttpStatus.UNAUTHORIZED);
  }
}
