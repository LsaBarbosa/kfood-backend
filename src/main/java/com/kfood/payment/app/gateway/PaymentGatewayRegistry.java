package com.kfood.payment.app.gateway;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayRegistry {

  private final Map<String, PaymentGateway> gatewaysByProvider;

  public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
    gatewaysByProvider =
        gateways.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    gateway -> normalize(gateway.providerCode()), Function.identity()));
  }

  public PaymentGateway resolve(String provider) {
    var gateway = gatewaysByProvider.get(normalize(provider));

    if (gateway == null) {
      throw new UnsupportedPaymentProviderException(provider);
    }

    return gateway;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
