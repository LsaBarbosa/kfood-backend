package com.kfood.payment.infra.gateway;

import com.kfood.payment.app.gateway.CreatePixChargeGatewayCommand;
import com.kfood.payment.app.gateway.CreatePixChargeGatewayResult;
import com.kfood.payment.app.gateway.PaymentGateway;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {

  @Override
  public String providerCode() {
    return "default-psp";
  }

  @Override
  public CreatePixChargeGatewayResult createPixCharge(CreatePixChargeGatewayCommand command) {
    return new CreatePixChargeGatewayResult(
        providerCode(),
        "mock-pay-" + command.paymentId(),
        "0002012636mockpix" + command.paymentId(),
        OffsetDateTime.now().plusMinutes(15));
  }
}
