package com.kfood.payment.api;

import com.kfood.payment.app.RegisterPaymentWebhookCommand;
import com.kfood.payment.app.RegisterPaymentWebhookUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments/webhooks")
public class PaymentWebhookController {

  private final RegisterPaymentWebhookUseCase registerPaymentWebhookUseCase;

  public PaymentWebhookController(RegisterPaymentWebhookUseCase registerPaymentWebhookUseCase) {
    this.registerPaymentWebhookUseCase = registerPaymentWebhookUseCase;
  }

  @PostMapping("/{provider}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void receive(@PathVariable String provider, @RequestBody String rawPayload) {
    registerPaymentWebhookUseCase.execute(new RegisterPaymentWebhookCommand(provider, rawPayload));
  }
}
