package com.kfood.customer.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.infra.persistence.Store;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldRejectNullStore() throws Exception {
    var customer = customer();
    setField(customer, "store", null);

    Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("store");
  }

  @Test
  void shouldRejectBlankEmailFormat() throws Exception {
    var customer = customer();
    setField(customer, "email", "invalid-email");

    Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email");
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

  private void setField(Customer customer, String fieldName, Object value) throws Exception {
    Field field = Customer.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(customer, value);
  }
}
