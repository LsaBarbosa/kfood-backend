package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.shared.config.AppProperties;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentWebhookAuthenticationServiceTest {

  @Test
  void shouldAuthenticateValidTokenForMockProvider() {
    var service = authenticationService(configuredProperties("mock-token"));

    assertThatCode(() -> service.authenticate("mock", "mock-token")).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectIncorrectTokenForMockProvider() {
    var service = authenticationService(configuredProperties("mock-token"));

    assertThatThrownBy(() -> service.authenticate("mock", "wrong-token"))
        .isInstanceOf(BusinessException.class)
        .satisfies(this::assertInvalidWebhookToken);
  }

  @Test
  void shouldRejectMissingTokenForMockProvider() {
    var service = authenticationService(configuredProperties("mock-token"));

    assertThatThrownBy(() -> service.authenticate("mock", null))
        .isInstanceOf(BusinessException.class)
        .satisfies(this::assertInvalidWebhookToken);
  }

  @Test
  void shouldRejectBlankTokenForMockProvider() {
    var service = authenticationService(configuredProperties("mock-token"));

    assertThatThrownBy(() -> service.authenticate("mock", "   "))
        .isInstanceOf(BusinessException.class)
        .satisfies(this::assertInvalidWebhookToken);
  }

  @Test
  void shouldRejectProviderWithoutAuthenticatorConfigured() {
    var service = authenticationService(configuredProperties("mock-token"));

    assertThatThrownBy(() -> service.authenticate("unknown", "mock-token"))
        .isInstanceOf(BusinessException.class)
        .satisfies(this::assertInvalidWebhookToken);
  }

  @Test
  void shouldRejectMockProviderWhenTokenConfigurationIsMissing() {
    var service = authenticationService(new AppProperties());

    assertThatThrownBy(() -> service.authenticate("mock", "mock-token"))
        .isInstanceOf(BusinessException.class)
        .satisfies(this::assertInvalidWebhookToken);
  }

  @Test
  void shouldRejectMockProviderWhenTokenConfigurationIsBlank() {
    var service = authenticationService(configuredProperties("   "));

    assertThatThrownBy(() -> service.authenticate("mock", "mock-token"))
        .isInstanceOf(BusinessException.class)
        .satisfies(this::assertInvalidWebhookToken);
  }

  private PaymentWebhookAuthenticationService authenticationService(AppProperties properties) {
    return new PaymentWebhookAuthenticationService(
        List.of(new MockPaymentWebhookAuthenticator(properties)));
  }

  private AppProperties configuredProperties(String token) {
    var properties = new AppProperties();
    properties.getPayment().getWebhook().getProviders().getMock().setToken(token);
    return properties;
  }

  private void assertInvalidWebhookToken(Throwable throwable) {
    var exception = (BusinessException) throwable;
    assertThatCode(() -> exception.getDetails()).doesNotThrowAnyException();
    org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
        .isEqualTo(ErrorCode.WEBHOOK_SIGNATURE_INVALID);
    org.assertj.core.api.Assertions.assertThat(exception.getStatus())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
