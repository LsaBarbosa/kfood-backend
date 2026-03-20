package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryZoneTest {

  @Test
  void shouldCreateValidZone() {
    var store = store();
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store,
            " Centro ",
            new BigDecimal("6.5"),
            new BigDecimal("25"),
            true);

    assertThat(zone.getId()).isNotNull();
    assertThat(zone.getStore()).isSameAs(store);
    assertThat(zone.getZoneName()).isEqualTo("Centro");
    assertThat(zone.getFeeAmount()).isEqualByComparingTo("6.50");
    assertThat(zone.getMinOrderAmount()).isEqualByComparingTo("25.00");
    assertThat(zone.isActive()).isTrue();
  }

  @Test
  void shouldChangeState() {
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store(),
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);

    zone.changeZoneName(" Bairro Novo ");
    zone.changeFeeAmount(new BigDecimal("8"));
    zone.changeMinOrderAmount(new BigDecimal("30"));
    zone.deactivate();

    assertThat(zone.getZoneName()).isEqualTo("Bairro Novo");
    assertThat(zone.getFeeAmount()).isEqualByComparingTo("8.00");
    assertThat(zone.getMinOrderAmount()).isEqualByComparingTo("30.00");
    assertThat(zone.isActive()).isFalse();

    zone.activate();
    assertThat(zone.isActive()).isTrue();
  }

  @Test
  void shouldRejectBlankZoneName() {
    assertThatThrownBy(
            () ->
                new DeliveryZone(
                    UUID.randomUUID(),
                    store(),
                    "   ",
                    new BigDecimal("6.50"),
                    new BigDecimal("25.00"),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("zoneName must not be blank");
  }

  @Test
  void shouldRejectNegativeFee() {
    assertThatThrownBy(
            () ->
                new DeliveryZone(
                    UUID.randomUUID(),
                    store(),
                    "Centro",
                    new BigDecimal("-1.00"),
                    new BigDecimal("25.00"),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("feeAmount must be greater than or equal to zero");
  }

  @Test
  void shouldRejectNegativeMinOrderAmount() {
    assertThatThrownBy(
            () ->
                new DeliveryZone(
                    UUID.randomUUID(),
                    store(),
                    "Centro",
                    new BigDecimal("6.50"),
                    new BigDecimal("-25.00"),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minOrderAmount must be greater than or equal to zero");
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var zone =
        new DeliveryZone(
            UUID.randomUUID(),
            store(),
            "Centro",
            new BigDecimal("6.50"),
            new BigDecimal("25.00"),
            true);
    Method prePersist = DeliveryZone.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);

    prePersist.invoke(zone);

    assertThat(zone.getZoneName()).isEqualTo("Centro");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<DeliveryZone> constructor = DeliveryZone.class.getDeclaredConstructor();
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
