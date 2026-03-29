package com.kfood.order.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CreatePixPaymentResponseTest {

  @ParameterizedTest
  @CsvSource({
    "CONFIRMED,PAID",
    "FAILED,FAILED",
    "CANCELED,FAILED",
    "EXPIRED,FAILED"
  })
  void shouldMapTechnicalStatusToSnapshot(String technicalStatus, String snapshotStatus) {
    var response =
        new CreatePixPaymentResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            PaymentMethod.PIX,
            PaymentStatus.valueOf(technicalStatus),
            "provider-ref",
            "000201pix",
            OffsetDateTime.parse("2026-03-29T12:00:00Z"));

    assertThat(response.paymentMethodSnapshot()).isEqualTo(PaymentMethod.PIX);
    assertThat(response.technicalPaymentStatus()).isEqualTo(PaymentStatus.valueOf(technicalStatus));
    assertThat(response.paymentStatusSnapshot())
        .isEqualTo(PaymentStatusSnapshot.valueOf(snapshotStatus));
  }
}
