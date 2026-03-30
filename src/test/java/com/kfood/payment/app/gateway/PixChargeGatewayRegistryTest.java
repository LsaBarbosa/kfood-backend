package com.kfood.payment.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PixChargeGatewayRegistryTest {

  @Test
  void shouldResolveRegisteredProvider() {
    var gateway = gateway("mock");
    var registry = new PixChargeGatewayRegistry(List.of(gateway));

    assertThat(registry.resolve("mock")).isSameAs(gateway);
  }

  @Test
  void shouldResolveRegisteredProviderIgnoringCaseAndWhitespace() {
    var gateway = gateway("mock");
    var registry = new PixChargeGatewayRegistry(List.of(gateway));

    assertThat(registry.resolve("  MOCK  ")).isSameAs(gateway);
  }

  @Test
  void shouldFailWhenProviderIsNotRegistered() {
    var registry = new PixChargeGatewayRegistry(List.of(gateway("mock")));

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
  void shouldFailFastWhenDuplicateProvidersAreRegisteredAfterNormalization() {
    assertThatThrownBy(
            () -> new PixChargeGatewayRegistry(List.of(gateway("mock"), gateway(" MOCK "))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate payment provider registered: mock");
  }

  @Test
  void shouldSupportBlankProviderCodeWhenRegisteringAndResolving() {
    var gateway = gateway("   ");
    var registry = new PixChargeGatewayRegistry(List.of(gateway));

    assertThat(registry.resolve("   ")).isSameAs(gateway);
  }

  @Test
  void shouldSupportNullProviderCodeWhenRegisteringAndResolving() {
    var gateway = gateway(null);
    var registry = new PixChargeGatewayRegistry(List.of(gateway));

    assertThat(registry.resolve(null)).isSameAs(gateway);
  }

  private PixChargeGateway gateway(String providerCode) {
    return new PixChargeGateway() {
      @Override
      public String providerCode() {
        return providerCode;
      }

      @Override
      public CreatePixChargeResponse createCharge(CreatePixChargeRequest request) {
        return new CreatePixChargeResponse(
            "charge-id",
            "provider-reference",
            "pix:payload",
            OffsetDateTime.parse("2030-01-01T00:00:00Z"));
      }
    };
  }
}
