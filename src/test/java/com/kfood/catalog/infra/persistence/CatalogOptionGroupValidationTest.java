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

class CatalogOptionGroupValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldRejectNullProduct() throws Exception {
    var optionGroup = optionGroup();
    setField(optionGroup, "product", null);

    Set<ConstraintViolation<CatalogOptionGroup>> violations = validator.validate(optionGroup);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("product");
  }

  @Test
  void shouldRejectNegativeMinSelect() throws Exception {
    var optionGroup = optionGroup();
    setField(optionGroup, "minSelect", -1);

    Set<ConstraintViolation<CatalogOptionGroup>> violations = validator.validate(optionGroup);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("minSelect");
  }

  private CatalogOptionGroup optionGroup() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    var category = new CatalogCategory(UUID.randomUUID(), store, "Pizzas", 10, true);
    var product =
        new CatalogProduct(
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

    return new CatalogOptionGroup(UUID.randomUUID(), product, "Sauces", 0, 2, false, true);
  }

  private void setField(CatalogOptionGroup optionGroup, String fieldName, Object value)
      throws Exception {
    Field field = CatalogOptionGroup.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(optionGroup, value);
  }
}
