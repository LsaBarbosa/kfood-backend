package com.kfood.catalog.infra.persistence;

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

class CatalogCategoryValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldRejectBlankName() throws Exception {
    var category = new CatalogCategory(UUID.randomUUID(), store(), "Pizzas", 10, true);
    setField(category, "name", "");

    Set<ConstraintViolation<CatalogCategory>> violations = validator.validate(category);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("name");
  }

  @Test
  void shouldRejectNegativeSortOrder() throws Exception {
    var category = new CatalogCategory(UUID.randomUUID(), store(), "Pizzas", 0, true);
    setField(category, "sortOrder", -1);

    Set<ConstraintViolation<CatalogCategory>> violations = validator.validate(category);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("sortOrder");
  }

  private void setField(CatalogCategory category, String fieldName, Object value) throws Exception {
    Field field = CatalogCategory.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(category, value);
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
