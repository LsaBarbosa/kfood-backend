package com.kfood.payment.app;

import com.kfood.payment.app.gateway.CreatePixChargeRequest;
import com.kfood.payment.app.gateway.PaymentGatewayErrorType;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.app.gateway.PixChargeGatewayRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(PixChargeGatewayRegistry.class)
public class CreatePixChargeUseCase {

  private final PixChargeGatewayRegistry gatewayRegistry;

  public CreatePixChargeUseCase(PixChargeGatewayRegistry gatewayRegistry) {
    this.gatewayRegistry = gatewayRegistry;
  }

  public PixChargeOutput execute(CreatePixChargeCommand command) {
    var gateway = gatewayRegistry.resolve(command.providerCode());
    try {
      var response =
          gateway.createCharge(
              new CreatePixChargeRequest(
                  command.paymentId(),
                  command.orderId(),
                  command.amount(),
                  command.idempotencyKey(),
                  command.correlationId(),
                  command.description()));

      return new PixChargeOutput(
          response.providerName(),
          response.providerReference(),
          response.qrCodePayload(),
          response.expiresAt());
    } catch (PaymentGatewayException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new PaymentGatewayException(
          command.providerCode(),
          PaymentGatewayErrorType.UNEXPECTED_ERROR,
          "Unexpected error while creating Pix charge.",
          ex);
    }
  }
}
