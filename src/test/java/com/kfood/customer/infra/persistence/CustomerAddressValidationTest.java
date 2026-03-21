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

class CustomerAddressValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldRejectNullCustomer() throws Exception {
    var address = address();
    setField(address, "customer", null);

    Set<ConstraintViolation<CustomerAddress>> violations = validator.validate(address);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("customer");
  }

  @Test
  void shouldRejectInvalidStateFormat() throws Exception {
    var address = address();
    setField(address, "state", "Rio");

    Set<ConstraintViolation<CustomerAddress>> violations = validator.validate(address);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("state");
  }

  private CustomerAddress address() {
    return new CustomerAddress(
        UUID.randomUUID(),
        new Customer(
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
            "maria@email.com"),
        "Casa",
        "25000000",
        "Rua das Flores",
        "45",
        "Centro",
        "Mage",
        "RJ",
        null,
        true);
  }

  private void setField(CustomerAddress address, String fieldName, Object value) throws Exception {
    Field field = CustomerAddress.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(address, value);
  }
}
