package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderItemOptionTest {

  @Test
  void shouldCreateOptionSnapshot() {
    var option =
        SalesOrderItemOption.create(UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 2);

    assertThat(option.getOptionNameSnapshot()).isEqualTo("Borda Catupiry");
    assertThat(option.getExtraPriceSnapshot()).isEqualByComparingTo("8.00");
    assertThat(option.getQuantity()).isEqualTo(2);
    assertThat(option.getTotalExtraAmount()).isEqualByComparingTo("16.00");
  }

  @Test
  void shouldRejectNegativeExtraPrice() {
    assertThatThrownBy(
            () ->
                SalesOrderItemOption.create(
                    UUID.randomUUID(), "Borda Catupiry", new BigDecimal("-1.00"), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraPriceSnapshot must not be negative");
  }

  @Test
  void shouldRejectBlankOptionName() {
    assertThatThrownBy(
            () -> SalesOrderItemOption.create(UUID.randomUUID(), " ", new BigDecimal("8.00"), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("optionNameSnapshot must not be blank");
  }

  @Test
  void shouldRejectQuantityLessThanOne() {
    assertThatThrownBy(
            () ->
                SalesOrderItemOption.create(UUID.randomUUID(), "Borda", new BigDecimal("8.00"), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("quantity must be greater than zero");
  }

  @Test
  void shouldRejectNullArguments() {
    assertThatThrownBy(() -> SalesOrderItemOption.create(null, "Borda", new BigDecimal("8.00"), 1))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("id is required");
    assertThatThrownBy(
            () -> SalesOrderItemOption.create(UUID.randomUUID(), null, new BigDecimal("8.00"), 1))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("optionNameSnapshot must not be null");
    assertThatThrownBy(() -> SalesOrderItemOption.create(UUID.randomUUID(), "Borda", null, 1))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("extraPriceSnapshot must not be null");
    assertThatThrownBy(
            () ->
                SalesOrderItemOption.create(
                    UUID.randomUUID(), "Borda", new BigDecimal("8.00"), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("quantity must not be null");
  }

  @Test
  void shouldValidateLifecycleAndRejectNullOrderItemAttachment() throws Exception {
    var option =
        SalesOrderItemOption.create(UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 2);
    Method method = SalesOrderItemOption.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);

    method.invoke(option);

    assertThatThrownBy(() -> option.attachToOrderItem(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("orderItem must not be null");
  }

  @Test
  void shouldRejectInvalidStateOnLifecycle() throws Exception {
    var option =
        SalesOrderItemOption.create(UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1);
    var method = SalesOrderItemOption.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);

    setField(option, "extraPriceSnapshot", new BigDecimal("-1.00"));
    assertThatThrownBy(() -> method.invoke(option))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("extraPriceSnapshot must not be negative");

    setField(option, "extraPriceSnapshot", new BigDecimal("8.00"));
    setField(option, "quantity", 0);
    assertThatThrownBy(() -> method.invoke(option))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("quantity must be greater than zero");

    setField(option, "quantity", 1);
    setField(option, "optionNameSnapshot", " ");
    assertThatThrownBy(() -> method.invoke(option))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("optionNameSnapshot must not be blank");
  }

  private void setField(SalesOrderItemOption option, String fieldName, Object value)
      throws Exception {
    Field field = SalesOrderItemOption.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(option, value);
  }
}
