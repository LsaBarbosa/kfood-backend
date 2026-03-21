package com.kfood.merchant.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreBusinessHourTest {

  @Test
  void shouldCreateOpenHour() {
    var store = store();
    var hour =
        StoreBusinessHour.open(store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0));

    assertThat(hour.getId()).isNotNull();
    assertThat(hour.getStore()).isSameAs(store);
    assertThat(hour.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(hour.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
    assertThat(hour.getCloseTime()).isEqualTo(LocalTime.of(22, 0));
    assertThat(hour.isClosed()).isFalse();
  }

  @Test
  void shouldCreateClosedHour() {
    var hour = StoreBusinessHour.closed(store(), DayOfWeek.SUNDAY);

    assertThat(hour.getOpenTime()).isNull();
    assertThat(hour.getCloseTime()).isNull();
    assertThat(hour.isClosed()).isTrue();
  }

  @Test
  void shouldRejectClosedDayWithTimes() {
    assertThatThrownBy(
            () ->
                invokeConstructor(
                    store(), DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), true))
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .rootCause()
        .hasMessage("closed day must not define openTime or closeTime");
  }

  @Test
  void shouldRejectOpenDayWithoutTimes() {
    assertThatThrownBy(() -> StoreBusinessHour.open(store(), DayOfWeek.MONDAY, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("openTime and closeTime are required for open day");
  }

  @Test
  void shouldRejectClosedDayWhenOnlyCloseTimeIsDefined() {
    assertThatThrownBy(
            () -> invokeConstructor(store(), DayOfWeek.SUNDAY, null, LocalTime.of(22, 0), true))
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .rootCause()
        .hasMessage("closed day must not define openTime or closeTime");
  }

  @Test
  void shouldRejectOpenDayWhenOnlyCloseTimeIsDefined() {
    assertThatThrownBy(
            () -> StoreBusinessHour.open(store(), DayOfWeek.MONDAY, null, LocalTime.of(22, 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("openTime and closeTime are required for open day");
  }

  @Test
  void shouldRejectOpenDayWhenOnlyOpenTimeIsDefined() {
    assertThatThrownBy(
            () -> StoreBusinessHour.open(store(), DayOfWeek.MONDAY, LocalTime.of(10, 0), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("openTime and closeTime are required for open day");
  }

  @Test
  void shouldRejectOpenTimeNotBeforeCloseTime() {
    assertThatThrownBy(
            () ->
                StoreBusinessHour.open(
                    store(), DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(10, 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("openTime must be before closeTime");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<StoreBusinessHour> constructor = StoreBusinessHour.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  @Test
  void shouldValidateOnPrePersist() throws Exception {
    var entity =
        invokeConstructor(
            store(), DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false);
    var method = StoreBusinessHour.class.getDeclaredMethod("prePersist");
    method.setAccessible(true);

    method.invoke(entity);

    assertThat(entity.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
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

  private StoreBusinessHour invokeConstructor(
      Store store, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed)
      throws Exception {
    Constructor<StoreBusinessHour> constructor =
        StoreBusinessHour.class.getDeclaredConstructor(
            UUID.class,
            Store.class,
            DayOfWeek.class,
            LocalTime.class,
            LocalTime.class,
            boolean.class);
    constructor.setAccessible(true);
    return constructor.newInstance(
        UUID.randomUUID(), store, dayOfWeek, openTime, closeTime, closed);
  }
}
