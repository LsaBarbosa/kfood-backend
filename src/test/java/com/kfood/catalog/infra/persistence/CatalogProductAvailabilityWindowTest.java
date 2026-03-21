package com.kfood.catalog.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.merchant.infra.persistence.Store;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogProductAvailabilityWindowTest {

  @Test
  void shouldCreateValidAvailabilityWindow() {
    var window =
        new CatalogProductAvailabilityWindow(
            UUID.randomUUID(),
            product(),
            DayOfWeek.MONDAY,
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            true);

    assertThat(window.getId()).isNotNull();
    assertThat(window.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(window.getStartTime()).isEqualTo(LocalTime.of(11, 0));
    assertThat(window.getEndTime()).isEqualTo(LocalTime.of(14, 0));
    assertThat(window.isActive()).isTrue();
  }

  @Test
  void shouldMatchOnlyWhenInsideWindow() {
    var window =
        new CatalogProductAvailabilityWindow(
            UUID.randomUUID(),
            product(),
            DayOfWeek.MONDAY,
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            true);

    assertThat(window.matches(DayOfWeek.MONDAY, LocalTime.of(11, 0))).isTrue();
    assertThat(window.matches(DayOfWeek.MONDAY, LocalTime.of(13, 59))).isTrue();
    assertThat(window.matches(DayOfWeek.MONDAY, LocalTime.of(14, 0))).isFalse();
    assertThat(window.matches(DayOfWeek.TUESDAY, LocalTime.of(12, 0))).isFalse();
  }

  @Test
  void shouldRejectInvalidTimeRange() {
    assertThatThrownBy(
            () ->
                new CatalogProductAvailabilityWindow(
                    UUID.randomUUID(),
                    product(),
                    DayOfWeek.MONDAY,
                    LocalTime.of(18, 0),
                    LocalTime.of(12, 0),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("startTime must be before endTime");
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var window =
        new CatalogProductAvailabilityWindow(
            UUID.randomUUID(),
            product(),
            DayOfWeek.MONDAY,
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            true);
    Method prePersist = CatalogProductAvailabilityWindow.class.getDeclaredMethod("prePersist");
    prePersist.setAccessible(true);

    prePersist.invoke(window);

    assertThat(window.getStartTime()).isEqualTo(LocalTime.of(11, 0));
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<CatalogProductAvailabilityWindow> constructor =
        CatalogProductAvailabilityWindow.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
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
}
