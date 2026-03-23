package com.kfood.payment.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentGatewayRegistryTest {

  @Test
  void shouldResolveGatewayUsingNormalizedProvider() {
    PaymentGateway gateway =
        new PaymentGateway() {
          @Override
          public String providerCode() {
            return " MOCK-PSP ";
          }

          @Override
          public CreatePixChargeGatewayResult createPixCharge(
              CreatePixChargeGatewayCommand command) {
            return new CreatePixChargeGatewayResult(
                "mock-psp", "ref", "qr", OffsetDateTime.parse("2030-01-01T00:00:00Z"));
          }
        };

    var registry = new PaymentGatewayRegistry(List.of(gateway));

    assertThat(registry.resolve(" mock-psp ")).isSameAs(gateway);
  }

  @Test
  void shouldRejectUnsupportedProviderEvenWhenNull() {
    var registry = new PaymentGatewayRegistry(List.of());

    assertThatThrownBy(() -> registry.resolve(null))
        .isInstanceOf(UnsupportedPaymentProviderException.class);
  }
}
