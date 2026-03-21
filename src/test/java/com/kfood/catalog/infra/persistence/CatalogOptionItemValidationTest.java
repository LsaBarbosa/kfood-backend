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

class CatalogOptionItemValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldRejectNullOptionGroup() throws Exception {
    var optionItem = optionItem();
    setField(optionItem, "optionGroup", null);

    Set<ConstraintViolation<CatalogOptionItem>> violations = validator.validate(optionItem);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("optionGroup");
  }

  @Test
  void shouldRejectNegativeSortOrder() throws Exception {
    var optionItem = optionItem();
    setField(optionItem, "sortOrder", -1);

    Set<ConstraintViolation<CatalogOptionItem>> violations = validator.validate(optionItem);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("sortOrder");
  }

  private CatalogOptionItem optionItem() {
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
    var optionGroup =
        new CatalogOptionGroup(UUID.randomUUID(), product, "Stuffed Crust", 0, 1, false, true);

    return new CatalogOptionItem(
        UUID.randomUUID(), optionGroup, "Catupiry", new BigDecimal("8.00"), true, 10);
  }

  private void setField(CatalogOptionItem optionItem, String fieldName, Object value)
      throws Exception {
    Field field = CatalogOptionItem.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(optionItem, value);
  }
}
