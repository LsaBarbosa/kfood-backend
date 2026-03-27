package com.kfood.payment.infra.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.payment.app.gateway.CreatePixChargeRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockPixChargeGatewayTest {

  private final MockPixChargeGateway gateway = new MockPixChargeGateway();

  @Test
  void shouldExposeStableProviderCode() {
    assertThat(gateway.providerCode()).isEqualTo("mock");
  }

  @Test
  void shouldReturnDeterministicPixChargeResponse() {
    var paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    var request =
        new CreatePixChargeRequest(
            paymentId,
            orderId,
            new BigDecimal("57.50"),
            "idem-123",
            "corr-456",
            "Pedido K-Food #123");

    var response = gateway.createCharge(request);

    assertThat(response.providerName()).isEqualTo("mock");
    assertThat(response.providerReference()).isEqualTo("mock-charge-" + paymentId);
    assertThat(response.qrCodePayload()).isEqualTo("pix:mock:" + paymentId + ":57.50");
    assertThat(response.expiresAt()).isEqualTo(OffsetDateTime.parse("2030-01-01T00:30:00Z"));
  }
}
