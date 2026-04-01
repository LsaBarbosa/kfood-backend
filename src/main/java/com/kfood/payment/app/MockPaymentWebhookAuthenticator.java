package com.kfood.payment.app;

import com.kfood.shared.config.AppProperties;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentWebhookAuthenticator implements PaymentWebhookAuthenticator {

  private static final String MOCK_PROVIDER = "mock";

  private final AppProperties appProperties;

  public MockPaymentWebhookAuthenticator(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public boolean supports(String provider) {
    return MOCK_PROVIDER.equals(provider);
  }

  @Override
  public void authenticate(String token) {
    var configuredToken =
        appProperties.getPayment().getWebhook().getProviders().getMock().getToken();
    if (configuredToken == null || configuredToken.isBlank() || token == null || token.isBlank()) {
      throw invalidWebhookToken();
    }
    if (!configuredToken.equals(token)) {
      throw invalidWebhookToken();
    }
  }

  private BusinessException invalidWebhookToken() {
    return new BusinessException(
        ErrorCode.WEBHOOK_SIGNATURE_INVALID, "Invalid webhook token.", HttpStatus.UNAUTHORIZED);
  }
}
