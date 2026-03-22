package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NoOpPaymentWebhookProcessorTest {

  @Test
  void shouldDoNothing() {
    var processor = new NoOpPaymentWebhookProcessor();
    var event =
        PaymentWebhookEvent.received(
            null, "mock-psp", "evt_001", "evt_001", "{\"type\":\"payment.confirmed\"}");
    var request =
        new PaymentWebhookRequest(
            "evt_001", "PAYMENT_CONFIRMED", "psp_ref_001", null, BigDecimal.TEN);

    assertThatCode(() -> processor.process(event, request)).doesNotThrowAnyException();
  }
}
