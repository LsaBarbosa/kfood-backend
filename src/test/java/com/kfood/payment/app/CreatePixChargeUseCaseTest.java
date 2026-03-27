package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.app.gateway.CreatePixChargeRequest;
import com.kfood.payment.app.gateway.CreatePixChargeResponse;
import com.kfood.payment.app.gateway.PaymentGatewayErrorType;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.app.gateway.PixChargeGateway;
import com.kfood.payment.app.gateway.PixChargeGatewayRegistry;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreatePixChargeUseCaseTest {

  private final PixChargeGatewayRegistry gatewayRegistry = mock(PixChargeGatewayRegistry.class);
  private final CreatePixChargeUseCase useCase = new CreatePixChargeUseCase(gatewayRegistry);

  @Test
  void shouldResolveProviderViaRegistryAndCreatePixCharge() {
    var gateway = mock(PixChargeGateway.class);
    var command = command("mock");
    var expiresAt = OffsetDateTime.parse("2030-01-01T00:30:00Z");

    when(gatewayRegistry.resolve("mock")).thenReturn(gateway);
    when(gateway.createCharge(any(CreatePixChargeRequest.class)))
        .thenReturn(
            new CreatePixChargeResponse(
                "mock", "charge-123", "pix:payload", expiresAt));

    var result = useCase.execute(command);

    ArgumentCaptor<CreatePixChargeRequest> captor =
        ArgumentCaptor.forClass(CreatePixChargeRequest.class);
    verify(gateway).createCharge(captor.capture());
    var request = captor.getValue();

    assertThat(request.paymentId()).isEqualTo(command.paymentId());
    assertThat(request.orderId()).isEqualTo(command.orderId());
    assertThat(request.amount()).isEqualByComparingTo(command.amount());
    assertThat(request.idempotencyKey()).isEqualTo(command.idempotencyKey());
    assertThat(request.correlationId()).isEqualTo(command.correlationId());
    assertThat(request.description()).isEqualTo(command.description());
    assertThat(result.providerName()).isEqualTo("mock");
    assertThat(result.providerReference()).isEqualTo("charge-123");
    assertThat(result.qrCodePayload()).isEqualTo("pix:payload");
    assertThat(result.expiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void shouldAllowProviderSwapWithoutChangingServiceCode() {
    var gateway = mock(PixChargeGateway.class);
    var command = command("sandbox");

    when(gatewayRegistry.resolve("sandbox")).thenReturn(gateway);
    when(gateway.createCharge(any(CreatePixChargeRequest.class)))
        .thenReturn(
            new CreatePixChargeResponse(
                "sandbox",
                "sandbox-ref",
                "pix:sandbox",
                OffsetDateTime.parse("2030-01-02T00:00:00Z")));

    var result = useCase.execute(command);

    verify(gatewayRegistry).resolve("sandbox");
    verify(gateway).createCharge(any(CreatePixChargeRequest.class));
    assertThat(result.providerName()).isEqualTo("sandbox");
    assertThat(result.providerReference()).isEqualTo("sandbox-ref");
  }

  @Test
  void shouldPropagateControlledGatewayFailure() {
    var gateway = mock(PixChargeGateway.class);
    var command = command("mock");
    var exception =
        new PaymentGatewayException(
            "mock", PaymentGatewayErrorType.PROVIDER_UNAVAILABLE, "Gateway unavailable");

    when(gatewayRegistry.resolve("mock")).thenReturn(gateway);
    when(gateway.createCharge(any(CreatePixChargeRequest.class))).thenThrow(exception);

    assertThatThrownBy(() -> useCase.execute(command))
        .isSameAs(exception);
  }

  @Test
  void shouldWrapUnexpectedGatewayFailureAsControlledError() {
    var gateway = mock(PixChargeGateway.class);
    var command = command("mock");
    var cause = new IllegalStateException("boom");

    when(gatewayRegistry.resolve("mock")).thenReturn(gateway);
    when(gateway.createCharge(any(CreatePixChargeRequest.class))).thenThrow(cause);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(PaymentGatewayException.class)
        .satisfies(
            throwable -> {
              var ex = (PaymentGatewayException) throwable;
              assertThat(ex.getProviderCode()).isEqualTo("mock");
              assertThat(ex.getErrorType()).isEqualTo(PaymentGatewayErrorType.UNEXPECTED_ERROR);
              assertThat(ex).hasMessage("Unexpected error while creating Pix charge.");
              assertThat(ex.getCause()).isSameAs(cause);
            });
  }

  private static CreatePixChargeCommand command(String providerCode) {
    return new CreatePixChargeCommand(
        providerCode,
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        new BigDecimal("57.50"),
        "idem-123",
        "corr-456",
        "Pedido K-Food #123");
  }
}
