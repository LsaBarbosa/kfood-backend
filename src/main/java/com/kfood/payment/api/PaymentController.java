package com.kfood.payment.api;

import com.kfood.payment.app.CreatePixPaymentCommand;
import com.kfood.payment.app.CreatePixPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders/{orderId}/payments")
public class PaymentController {

  private final CreatePixPaymentUseCase createPixPaymentUseCase;

  public PaymentController(CreatePixPaymentUseCase createPixPaymentUseCase) {
    this.createPixPaymentUseCase = createPixPaymentUseCase;
  }

  @PostMapping("/pix")
  @ResponseStatus(HttpStatus.CREATED)
  public CreatePixPaymentResponse createPixPayment(
      @PathVariable java.util.UUID orderId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreatePixPaymentRequest request) {
    return createPixPaymentUseCase.execute(
        new CreatePixPaymentCommand(orderId, request.amount(), request.provider(), idempotencyKey));
  }
}
