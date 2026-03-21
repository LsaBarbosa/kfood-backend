package com.kfood.customer.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTest {

  @Test
  void shouldCreateValidCustomer() {
    var customer =
        new Customer(UUID.randomUUID(), store(), " Maria ", " 21999990000 ", " Maria@Email.com ");

    assertThat(customer.getName()).isEqualTo("Maria");
    assertThat(customer.getPhone()).isEqualTo("21999990000");
    assertThat(customer.getEmail()).isEqualTo("maria@email.com");
  }

  @Test
  void shouldUpdateCustomer() {
    var customer =
        new Customer(UUID.randomUUID(), store(), "Maria", "21999990000", "maria@email.com");

    customer.update("Maria Silva", null, "maria.silva@email.com");

    assertThat(customer.getName()).isEqualTo("Maria Silva");
    assertThat(customer.getPhone()).isNull();
    assertThat(customer.getEmail()).isEqualTo("maria.silva@email.com");
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(() -> new Customer(UUID.randomUUID(), store(), " ", "21999990000", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must not be blank");
  }

  @Test
  void shouldRejectWhenPhoneAndEmailAreMissing() {
    assertThatThrownBy(() -> new Customer(UUID.randomUUID(), store(), "Maria", " ", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("phone or email must be informed");
  }

  @Test
  void shouldValidateOnLifecycle() throws Exception {
    var customer =
        new Customer(UUID.randomUUID(), store(), "Maria", "21999990000", "maria@email.com");
    Method method = Customer.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);

    method.invoke(customer);

    assertThat(customer.getEmail()).isEqualTo("maria@email.com");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<Customer> constructor = Customer.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private Store store() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }
}
