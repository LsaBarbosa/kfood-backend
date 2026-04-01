package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.domain.StoreStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoreValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldApplySetupAsDefaultStatus() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    assertThat(store.getStatus()).isEqualTo(StoreStatus.SETUP);
  }

  @Test
  void shouldRejectInvalidCnpj() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "11.111.111/1111-11",
            "21999990000",
            "America/Sao_Paulo");

    Set<ConstraintViolation<Store>> violations = validator.validate(store);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("cnpj");
  }

  @Test
  void shouldRejectBlankName() {
    var store =
        new Store(
            UUID.randomUUID(),
            "",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");

    Set<ConstraintViolation<Store>> violations = validator.validate(store);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("name");
  }
}
