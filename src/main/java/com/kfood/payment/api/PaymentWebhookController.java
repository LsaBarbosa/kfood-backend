package com.kfood.payment.api;

import com.kfood.payment.app.PaymentWebhookAuthenticationService;
import com.kfood.payment.app.RegisterPaymentWebhookCommand;
import com.kfood.payment.app.RegisterPaymentWebhookUseCase;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments/webhooks")
public class PaymentWebhookController {

  private static final String WEBHOOK_TOKEN_HEADER = "X-Webhook-Token";

  private final PaymentWebhookAuthenticationService paymentWebhookAuthenticationService;
  private final RegisterPaymentWebhookUseCase registerPaymentWebhookUseCase;

  public PaymentWebhookController(
      PaymentWebhookAuthenticationService paymentWebhookAuthenticationService,
      RegisterPaymentWebhookUseCase registerPaymentWebhookUseCase) {
    this.paymentWebhookAuthenticationService = paymentWebhookAuthenticationService;
    this.registerPaymentWebhookUseCase = registerPaymentWebhookUseCase;
  }

  @PostMapping("/{provider}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public PaymentWebhookResponse receive(
      @PathVariable String provider,
      @RequestHeader(name = WEBHOOK_TOKEN_HEADER, required = false) String webhookToken,
      @RequestBody String rawPayload) {
    var signatureValid = paymentWebhookAuthenticationService.isValid(provider, webhookToken);
    var result =
        registerPaymentWebhookUseCase.execute(
            new RegisterPaymentWebhookCommand(provider, rawPayload, signatureValid));
    if (!signatureValid) {
      throw new BusinessException(
          ErrorCode.WEBHOOK_SIGNATURE_INVALID, "Invalid webhook token.", HttpStatus.UNAUTHORIZED);
    }
    return PaymentWebhookResponse.from(result);
  }
}
