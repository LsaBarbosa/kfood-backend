package com.kfood.payment.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.infra.gateway.MockPixChargeGateway;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PixChargeGatewayRegistryTest {

  @Test
  void shouldResolveRegisteredProviderIgnoringCaseAndWhitespace() {
    var gateway = new MockPixChargeGateway();
    var registry = new PixChargeGatewayRegistry(List.of(gateway));

    var resolved = registry.resolve("  MOCK  ");

    assertThat(resolved).isSameAs(gateway);
  }

  @Test
  void shouldFailWhenProviderIsNotRegistered() {
    var registry = new PixChargeGatewayRegistry(List.of(new MockPixChargeGateway()));

    assertThatThrownBy(() -> registry.resolve("pix-sandbox"))
        .isInstanceOf(UnsupportedPaymentProviderException.class)
        .satisfies(
            throwable -> {
              var ex = (UnsupportedPaymentProviderException) throwable;
              assertThat(ex.getProviderCode()).isEqualTo("pix-sandbox");
              assertThat(ex.getErrorType())
                  .isEqualTo(PaymentGatewayErrorType.PROVIDER_NOT_SUPPORTED);
            });
  }

  @Test
  void shouldFailFastWhenDuplicateProvidersAreRegistered() {
    PixChargeGateway duplicateGateway =
        new PixChargeGateway() {
          @Override
          public String providerCode() {
            return MockPixChargeGateway.PROVIDER_CODE;
          }

          @Override
          public CreatePixChargeResponse createCharge(CreatePixChargeRequest request) {
            return new CreatePixChargeResponse(
                "duplicate",
                "duplicate-" + request.paymentId(),
                "pix:duplicate",
                OffsetDateTime.parse("2030-01-01T00:00:00Z"));
          }
        };

    assertThatThrownBy(
            () ->
                new PixChargeGatewayRegistry(List.of(new MockPixChargeGateway(), duplicateGateway)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate payment provider registered: mock");
  }
}
