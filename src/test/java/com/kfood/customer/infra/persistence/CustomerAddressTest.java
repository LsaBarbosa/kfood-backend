package com.kfood.customer.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerAddressTest {

  @Test
  void shouldCreateValidAddress() {
    var address =
        new CustomerAddress(
            UUID.randomUUID(),
            customer(),
            " Casa ",
            "25000-000",
            " Rua das Flores ",
            " 45 ",
            " Centro ",
            " Mage ",
            " rj ",
            " Ap 101 ",
            true);

    assertThat(address.getLabel()).isEqualTo("Casa");
    assertThat(address.getZipCode()).isEqualTo("25000000");
    assertThat(address.getStreet()).isEqualTo("Rua das Flores");
    assertThat(address.getNumber()).isEqualTo("45");
    assertThat(address.getDistrict()).isEqualTo("Centro");
    assertThat(address.getCity()).isEqualTo("Mage");
    assertThat(address.getState()).isEqualTo("RJ");
    assertThat(address.getComplement()).isEqualTo("Ap 101");
    assertThat(address.isMainAddress()).isTrue();
  }

  @Test
  void shouldRejectInvalidZipCode() {
    assertThatThrownBy(
            () ->
                new CustomerAddress(
                    UUID.randomUUID(),
                    customer(),
                    "Casa",
                    "2500-000",
                    "Rua das Flores",
                    "45",
                    "Centro",
                    "Mage",
                    "RJ",
                    null,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("zipCode must contain 8 digits");
  }

  @Test
  void shouldValidateOnLifecycle() throws Exception {
    var address =
        new CustomerAddress(
            UUID.randomUUID(),
            customer(),
            "Casa",
            "25000000",
            "Rua das Flores",
            "45",
            "Centro",
            "Mage",
            "RJ",
            null,
            false);
    Method method = CustomerAddress.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);

    method.invoke(address);

    assertThat(address.getZipCode()).isEqualTo("25000000");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<CustomerAddress> constructor = CustomerAddress.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private Customer customer() {
    return new Customer(
        UUID.randomUUID(),
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo"),
        "Maria",
        "21999990000",
        "maria@email.com");
  }
}
