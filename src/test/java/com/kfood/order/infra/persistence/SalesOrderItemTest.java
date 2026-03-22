package com.kfood.order.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.payment.domain.PaymentMethod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderItemTest {

  @Test
  void shouldCreateItemWithFrozenSnapshot() {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            2,
            "Sem cebola");

    item.addOption(
        SalesOrderItemOption.create(
            UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 1));

    assertThat(item.getProductNameSnapshot()).isEqualTo("Pizza Calabresa");
    assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("42.00");
    assertThat(item.getQuantity()).isEqualTo(2);
    assertThat(item.getOptions()).hasSize(1);
    assertThat(item.getTotalItemAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldRejectItemWithoutProductId() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(), null, "Pizza Calabresa", new BigDecimal("42.00"), 1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("productId must not be null");
  }

  @Test
  void shouldRejectNegativeUnitPrice() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    new BigDecimal("-1.00"),
                    1,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unitPriceSnapshot must not be negative");
  }

  @Test
  void shouldRejectQuantityLessThanOne() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Pizza Calabresa",
                    new BigDecimal("42.00"),
                    0,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("quantity must be greater than zero");
  }

  @Test
  void shouldAttachOptionAndExposeDerivedValues() {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            " ");
    var option =
        SalesOrderItemOption.create(UUID.randomUUID(), "Borda Catupiry", new BigDecimal("8.00"), 2);

    item.addOption(option);

    assertThat(item.getId()).isNotNull();
    assertThat(item.getProductId()).isNotNull();
    assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("42.00");
    assertThat(item.getQuantity()).isEqualTo(1);
    assertThat(item.getNotes()).isNull();
    assertThat(item.getOptions()).containsExactly(option);
    assertThat(item.getOptionExtrasPerUnit()).isEqualByComparingTo("16.00");
    assertThat(item.getTotalItemAmount()).isEqualByComparingTo("58.00");
    assertThat(option.getOrderItem()).isEqualTo(item);
  }

  @Test
  void shouldAttachItemToOrder() {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);
    var order =
        SalesOrder.create(
            UUID.randomUUID(),
            mock(Store.class),
            mock(Customer.class),
            FulfillmentType.PICKUP,
            PaymentMethod.PIX,
            new BigDecimal("42.00"),
            BigDecimal.ZERO,
            new BigDecimal("42.00"),
            null,
            null);

    order.addItem(item);

    assertThat(item.getOrder()).isEqualTo(order);
  }

  @Test
  void shouldRejectBlankProductNameOnLifecycle() throws Exception {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);
    setField(item, "productNameSnapshot", " ");

    assertThatThrownBy(() -> invokeValidateLifecycle(item))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("productNameSnapshot must not be blank");
  }

  @Test
  void shouldRejectNegativeTotalItemAmountOnLifecycle() throws Exception {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);
    setField(item, "totalItemAmount", new BigDecimal("-1.00"));

    assertThatThrownBy(() -> invokeValidateLifecycle(item))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("totalItemAmount must not be negative");
  }

  @Test
  void shouldRejectNullProductName() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(), UUID.randomUUID(), null, new BigDecimal("42.00"), 1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("productNameSnapshot must not be null");
  }

  @Test
  void shouldRejectBlankProductNameOnCreation() {
    assertThatThrownBy(
            () ->
                SalesOrderItem.create(
                    UUID.randomUUID(), UUID.randomUUID(), "   ", new BigDecimal("42.00"), 1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("productNameSnapshot must not be blank");
  }

  @Test
  void shouldExposeZeroExtrasWhenThereAreNoOptions() {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);

    assertThat(item.getOptionExtrasPerUnit()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldRejectNullOption() {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);

    assertThatThrownBy(() -> item.addOption(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("option must not be null");
  }

  @Test
  void shouldRejectOtherInvalidLifecycleStates() throws Exception {
    var item =
        SalesOrderItem.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pizza Calabresa",
            new BigDecimal("42.00"),
            1,
            null);

    setField(item, "productId", null);
    assertThatThrownBy(() -> invokeValidateLifecycle(item))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("productId must not be null");

    setField(item, "productId", UUID.randomUUID());
    setField(item, "unitPriceSnapshot", new BigDecimal("-1.00"));
    assertThatThrownBy(() -> invokeValidateLifecycle(item))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("unitPriceSnapshot must not be negative");

    setField(item, "unitPriceSnapshot", new BigDecimal("42.00"));
    setField(item, "quantity", 0);
    assertThatThrownBy(() -> invokeValidateLifecycle(item))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("quantity must be greater than zero");
  }

  private void invokeValidateLifecycle(SalesOrderItem item) throws Exception {
    Method method = SalesOrderItem.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);
    method.invoke(item);
  }

  private void setField(SalesOrderItem item, String fieldName, Object value) throws Exception {
    Field field = SalesOrderItem.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(item, value);
  }
}
