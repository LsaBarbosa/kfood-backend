package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StoreActivationRequirementsTest {

  @Test
  void shouldAllowActivationWhenAllRequirementsAreMet() {
    var requirements = new StoreActivationRequirements(true, true, true);

    assertThat(requirements.canActivate()).isTrue();
    assertThat(requirements.missingRequirements()).isEmpty();
  }

  @Test
  void shouldListMissingDeliveryZonesRequirement() {
    var requirements = new StoreActivationRequirements(true, false, true);

    assertThat(requirements.canActivate()).isFalse();
    assertThat(requirements.missingRequirements()).containsExactly("deliveryZonesConfigured");
  }
}
