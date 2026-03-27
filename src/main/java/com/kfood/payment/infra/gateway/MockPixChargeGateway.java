package com.kfood.payment.infra.gateway;

import com.kfood.payment.app.gateway.CreatePixChargeRequest;
import com.kfood.payment.app.gateway.CreatePixChargeResponse;
import com.kfood.payment.app.gateway.PixChargeGateway;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class MockPixChargeGateway implements PixChargeGateway {

  public static final String PROVIDER_CODE = "mock";
  private static final OffsetDateTime DEFAULT_EXPIRATION = OffsetDateTime.parse("2030-01-01T00:30:00Z");

  @Override
  public String providerCode() {
    return PROVIDER_CODE;
  }

  @Override
  public CreatePixChargeResponse createCharge(CreatePixChargeRequest request) {
    return new CreatePixChargeResponse(
        PROVIDER_CODE,
        "mock-charge-" + request.paymentId(),
        "pix:mock:" + request.paymentId() + ":" + request.amount(),
        DEFAULT_EXPIRATION);
  }
}
