package com.kfood.merchant.app;

import java.util.ArrayList;
import java.util.List;

public record StoreActivationRequirements(
    boolean categoryConfigured,
    boolean addressConfigured,
    boolean hoursConfigured,
    boolean deliveryZonesConfigured,
    boolean termsAccepted) {

  public StoreActivationRequirements(
      boolean hoursConfigured, boolean deliveryZonesConfigured, boolean termsAccepted) {
    this(true, true, hoursConfigured, deliveryZonesConfigured, termsAccepted);
  }

  public boolean canActivate() {
    return categoryConfigured
        && addressConfigured
        && hoursConfigured
        && deliveryZonesConfigured
        && termsAccepted;
  }

  public List<String> missingRequirements() {
    var missing = new ArrayList<String>();
    if (!categoryConfigured) {
      missing.add("category");
    }
    if (!addressConfigured) {
      missing.add("address");
    }
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
