package com.kfood.payment.app;

import com.kfood.payment.app.gateway.CreatePixChargeGatewayCommand;
import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.app.gateway.PaymentGatewayRegistry;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(PaymentGatewayRegistry.class)
public class RequestPixChargeViaGatewayUseCase {

  private final PaymentGatewayRegistry paymentGatewayRegistry;

  public RequestPixChargeViaGatewayUseCase(PaymentGatewayRegistry paymentGatewayRegistry) {
    this.paymentGatewayRegistry = paymentGatewayRegistry;
  }

  public CreatePixChargeGatewayResult execute(RequestPixChargeViaGatewayCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.provider(), "provider must not be null");

    var gateway = paymentGatewayRegistry.resolve(command.provider());
    var gatewayCommand =
        new CreatePixChargeGatewayCommand(
            command.paymentId(),
            command.orderId(),
            command.amount(),
            command.idempotencyKey(),
            command.correlationId(),
            command.description());

    try {
      return gateway.createPixCharge(gatewayCommand);
    } catch (PaymentGatewayException exception) {
      throw new BusinessException(
          ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE,
          "Payment provider is unavailable.",
          HttpStatus.SERVICE_UNAVAILABLE);
    } catch (RuntimeException exception) {
      throw new BusinessException(
          ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE,
          "Payment provider is unavailable.",
          HttpStatus.SERVICE_UNAVAILABLE);
    }
  }
}
