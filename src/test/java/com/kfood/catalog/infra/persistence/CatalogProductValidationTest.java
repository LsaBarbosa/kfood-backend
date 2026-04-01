package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.merchant.infra.persistence.Store;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogProductValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldRejectNullCategory() throws Exception {
    var product = product();
    setField(product, "category", null);

    Set<ConstraintViolation<CatalogProduct>> violations = validator.validate(product);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("category");
  }

  @Test
  void shouldRejectNegativeBasePrice() throws Exception {
    var product = product();
    setField(product, "basePrice", new BigDecimal("-1.00"));

    Set<ConstraintViolation<CatalogProduct>> violations = validator.validate(product);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("basePrice");
  }

  private CatalogProduct product() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);

    return new CatalogProduct(
        UUID.randomUUID(),
        store,
        category,
        "Pizza Calabresa",
        "Pizza com calabresa e cebola",
        new BigDecimal("39.90"),
        null,
        20,
        true,
        false);
  }

  private void setField(CatalogProduct product, String fieldName, Object value) throws Exception {
    Field field = CatalogProduct.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(product, value);
  }
}
