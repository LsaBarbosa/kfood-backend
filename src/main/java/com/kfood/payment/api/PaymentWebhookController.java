package com.kfood.payment.api;

import com.kfood.payment.app.PaymentWebhookIngressService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments/webhooks")
public class PaymentWebhookController {

  private final PaymentWebhookIngressService paymentWebhookIngressService;

  public PaymentWebhookController(PaymentWebhookIngressService paymentWebhookIngressService) {
    this.paymentWebhookIngressService = paymentWebhookIngressService;
  }

  @PostMapping(
      value = "/{provider}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PaymentWebhookResponse> receiveWebhook(
      @PathVariable String provider,
      @RequestHeader HttpHeaders headers,
      @RequestBody(required = false) String rawPayload) {
    var response = paymentWebhookIngressService.receive(provider, headers, rawPayload);
    return ResponseEntity.accepted().body(response);
  }
}
