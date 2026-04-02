package com.kfood.merchant.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StoreActivationRequirementsTest {

  @Test
  void shouldAllowActivationWhenAllRequirementsAreMet() {
    var requirements = new StoreActivationRequirements(true, true, true, true, true);

    assertThat(requirements.canActivate()).isTrue();
    assertThat(requirements.missingRequirements()).isEmpty();
  }

  @Test
  void shouldListMissingCategoryAddressAndDeliveryZonesRequirements() {
    var requirements = new StoreActivationRequirements(false, false, true, false, true);

    assertThat(requirements.canActivate()).isFalse();
    assertThat(requirements.missingRequirements())
        .containsExactly("category", "address", "deliveryZonesConfigured");
  }

  @Test
  void shouldKeepLegacyConstructorCompatible() {
    var requirements = new StoreActivationRequirements(true, false, true);

    assertThat(requirements.categoryConfigured()).isTrue();
    assertThat(requirements.addressConfigured()).isTrue();
    assertThat(requirements.hoursConfigured()).isTrue();
    assertThat(requirements.deliveryZonesConfigured()).isFalse();
    assertThat(requirements.termsAccepted()).isTrue();
  }

  @Test
  void shouldListMissingTermsRequirement() {
    var requirements = new StoreActivationRequirements(true, true, true, true, false);

    assertThat(requirements.canActivate()).isFalse();
    assertThat(requirements.missingRequirements()).containsExactly("termsAccepted");
  }
}
