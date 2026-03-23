package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.app.gateway.CreatePixChargeGatewayCommand;
import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.payment.app.gateway.PaymentGateway;
import com.kfood.payment.app.gateway.PaymentGatewayErrorType;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.app.gateway.PaymentGatewayRegistry;
import com.kfood.payment.app.gateway.UnsupportedPaymentProviderException;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RequestPixChargeViaGatewayUseCaseTest {

  @Test
  void shouldUseGatewayInterfaceInsteadOfConcreteImplementation() {
    var paymentId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    PaymentGateway fakeGateway =
        new PaymentGateway() {
          @Override
          public String providerCode() {
            return "fake-a";
          }

          @Override
          public CreatePixChargeGatewayResult createPixCharge(
              CreatePixChargeGatewayCommand command) {
            return new CreatePixChargeGatewayResult(
                "fake-a",
                "ref-a-" + command.paymentId(),
                "pix-a-" + command.paymentId(),
                OffsetDateTime.parse("2026-03-22T12:00:00Z"));
          }
        };
    var useCase =
        new RequestPixChargeViaGatewayUseCase(new PaymentGatewayRegistry(List.of(fakeGateway)));

    var result =
        useCase.execute(
            new RequestPixChargeViaGatewayCommand(
                "fake-a",
                paymentId,
                orderId,
                new BigDecimal("57.90"),
                "idem-1",
                "corr-1",
                "Order 100"));

    assertThat(result.providerName()).isEqualTo("fake-a");
    assertThat(result.providerReference()).isEqualTo("ref-a-" + paymentId);
    assertThat(result.qrCodePayload()).isEqualTo("pix-a-" + paymentId);
  }

  @Test
  void shouldAllowReplacingGatewayWithoutChangingBusinessService() {
    PaymentGateway gatewayA =
        new PaymentGateway() {
          @Override
          public String providerCode() {
            return "default-psp";
          }

          @Override
          public CreatePixChargeGatewayResult createPixCharge(
              CreatePixChargeGatewayCommand command) {
            return new CreatePixChargeGatewayResult(
                "default-psp", "mock-ref", "mock-qr", OffsetDateTime.parse("2026-03-22T12:05:00Z"));
          }
        };
    PaymentGateway gatewayB =
        new PaymentGateway() {
          @Override
          public String providerCode() {
            return "sandbox-psp";
          }

          @Override
          public CreatePixChargeGatewayResult createPixCharge(
              CreatePixChargeGatewayCommand command) {
            return new CreatePixChargeGatewayResult(
                "sandbox-psp",
                "sandbox-ref",
                "sandbox-qr",
                OffsetDateTime.parse("2026-03-22T12:10:00Z"));
          }
        };
    var useCase =
        new RequestPixChargeViaGatewayUseCase(
            new PaymentGatewayRegistry(List.of(gatewayA, gatewayB)));

    var responseA =
        useCase.execute(
            new RequestPixChargeViaGatewayCommand(
                "default-psp",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                "idem-a",
                "corr-a",
                "Order A"));
    var responseB =
        useCase.execute(
            new RequestPixChargeViaGatewayCommand(
                "sandbox-psp",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("20.00"),
                "idem-b",
                "corr-b",
                "Order B"));

    assertThat(responseA.providerName()).isEqualTo("default-psp");
    assertThat(responseB.providerName()).isEqualTo("sandbox-psp");
  }

  @Test
  void shouldConvertControlledGatewayErrorToBusinessException() {
    PaymentGateway failingGateway =
        new PaymentGateway() {
          @Override
          public String providerCode() {
            return "timeout-psp";
          }

          @Override
          public CreatePixChargeGatewayResult createPixCharge(
              CreatePixChargeGatewayCommand command) {
            throw PaymentGatewayException.timeout(
                "timeout-psp", "PSP timeout while creating Pix charge", null);
          }
        };
    var useCase =
        new RequestPixChargeViaGatewayUseCase(new PaymentGatewayRegistry(List.of(failingGateway)));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RequestPixChargeViaGatewayCommand(
                        "timeout-psp",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("30.00"),
                        "idem-timeout",
                        "corr-timeout",
                        "Order timeout")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void shouldWrapUnexpectedGatewayFailureAsControlledError() {
    PaymentGateway brokenGateway =
        new PaymentGateway() {
          @Override
          public String providerCode() {
            return "broken-psp";
          }

          @Override
          public CreatePixChargeGatewayResult createPixCharge(
              CreatePixChargeGatewayCommand command) {
            throw new IllegalStateException("low level http client exploded");
          }
        };
    var useCase =
        new RequestPixChargeViaGatewayUseCase(new PaymentGatewayRegistry(List.of(brokenGateway)));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RequestPixChargeViaGatewayCommand(
                        "broken-psp",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("40.00"),
                        "idem-broken",
                        "corr-broken",
                        "Order broken")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void shouldFailWhenProviderIsUnsupported() {
    var useCase = new RequestPixChargeViaGatewayUseCase(new PaymentGatewayRegistry(List.of()));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RequestPixChargeViaGatewayCommand(
                        "unknown-psp",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("50.00"),
                        "idem-unknown",
                        "corr-unknown",
                        "Order unknown")))
        .isInstanceOf(UnsupportedPaymentProviderException.class)
        .extracting("errorCode", "status")
        .containsExactly(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldExposeGatewayExceptionMetadata() {
    var exception =
        PaymentGatewayException.invalidResponse(
            "default-psp", "Gateway returned invalid response", null);

    assertThat(exception.getProvider()).isEqualTo("default-psp");
    assertThat(exception.getErrorType()).isEqualTo(PaymentGatewayErrorType.INVALID_RESPONSE);
  }
}
