package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StoreAddressTest {

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    var constructor = StoreAddress.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var address = constructor.newInstance();

    assertThat(address).isNotNull();
  }

  @Test
  void shouldNormalizeAddressFields() {
    var address =
        new StoreAddress("25000-000", " Rua Central ", " 100 ", " Centro ", " Mage ", "rj");

    assertThat(address.getZipCode()).isEqualTo("25000000");
    assertThat(address.getStreet()).isEqualTo("Rua Central");
    assertThat(address.getNumber()).isEqualTo("100");
    assertThat(address.getDistrict()).isEqualTo("Centro");
    assertThat(address.getCity()).isEqualTo("Mage");
    assertThat(address.getState()).isEqualTo("RJ");
    assertThat(address.isComplete()).isTrue();
  }

  @Test
  void shouldRejectInvalidZipCode() {
    assertThatThrownBy(() -> new StoreAddress("2500", "Rua Central", "100", "Centro", "Mage", "RJ"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("zipCode");
  }

  @Test
  void shouldRejectBlankZipCodeAfterNormalization() {
    assertThatThrownBy(() -> new StoreAddress("---", "Rua Central", "100", "Centro", "Mage", "RJ"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("zipCode");
  }

  @Test
  void shouldRejectInvalidState() {
    assertThatThrownBy(
            () -> new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", "Rio"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("state");
  }

  @Test
  void shouldRejectBlankStateAfterNormalization() {
    assertThatThrownBy(
            () -> new StoreAddress("25000-000", "Rua Central", "100", "Centro", "Mage", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("state");
  }
}
