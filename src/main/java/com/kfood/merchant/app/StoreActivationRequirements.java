package com.kfood.merchant.app;

import java.util.ArrayList;
import java.util.List;

public record StoreActivationRequirements(
    boolean hoursConfigured, boolean deliveryZonesConfigured, boolean termsAccepted) {

  public boolean canActivate() {
    return hoursConfigured && deliveryZonesConfigured && termsAccepted;
  }

  public List<String> missingRequirements() {
    var missing = new ArrayList<String>();
    if (!hoursConfigured) {
      missing.add("hoursConfigured");
    }
    if (!deliveryZonesConfigured) {
      missing.add("deliveryZonesConfigured");
    }
    if (!termsAccepted) {
      missing.add("termsAccepted");
    }
    return List.copyOf(missing);
  }
}
