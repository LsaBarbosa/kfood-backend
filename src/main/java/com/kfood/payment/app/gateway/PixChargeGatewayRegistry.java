package com.kfood.payment.app.gateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PixChargeGatewayRegistry {

  private final Map<String, PixChargeGateway> gatewaysByProviderCode;

  public PixChargeGatewayRegistry(List<PixChargeGateway> gateways) {
    this.gatewaysByProviderCode = indexByProviderCode(gateways);
  }

  public PixChargeGateway resolve(String providerCode) {
    var normalizedProviderCode = normalizeProviderCode(providerCode);
    var gateway = gatewaysByProviderCode.get(normalizedProviderCode);
    if (gateway == null) {
      throw new UnsupportedPaymentProviderException(providerCode);
    }
    return gateway;
  }

  private static Map<String, PixChargeGateway> indexByProviderCode(
      List<PixChargeGateway> gateways) {
    Map<String, PixChargeGateway> indexed = new LinkedHashMap<>();
    for (var gateway : gateways) {
      var providerCode = normalizeProviderCode(gateway.providerCode());
      var existing = indexed.putIfAbsent(providerCode, gateway);
      if (existing != null) {
        throw new IllegalStateException("Duplicate payment provider registered: " + providerCode);
      }
    }
    return Map.copyOf(indexed);
  }

  private static String normalizeProviderCode(String providerCode) {
    if (providerCode == null || providerCode.isBlank()) {
      return "";
    }
    return providerCode.trim().toLowerCase(Locale.ROOT);
  }
}
