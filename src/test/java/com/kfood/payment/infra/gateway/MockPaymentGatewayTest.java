package com.kfood.payment.infra.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.payment.app.gateway.CreatePixChargeGatewayCommand;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockPaymentGatewayTest {

  @Test
  void shouldReturnDefaultProviderAndMockPixCharge() {
    var gateway = new MockPaymentGateway();
    var paymentId = UUID.randomUUID();
    var command =
        new CreatePixChargeGatewayCommand(
            paymentId,
            UUID.randomUUID(),
            new BigDecimal("57.90"),
            "idem-1",
            "corr-1",
            "Pix charge");

    var response = gateway.createPixCharge(command);

    assertThat(gateway.providerCode()).isEqualTo("default-psp");
    assertThat(response.providerName()).isEqualTo("default-psp");
    assertThat(response.providerReference()).isEqualTo("mock-pay-" + paymentId);
    assertThat(response.qrCodePayload()).isEqualTo("0002012636mockpix" + paymentId);
    assertThat(response.expiresAt()).isAfter(OffsetDateTime.now().plusMinutes(14));
  }
}
