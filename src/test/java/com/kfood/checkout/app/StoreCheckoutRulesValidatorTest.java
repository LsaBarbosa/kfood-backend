package com.kfood.checkout.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.merchant.infra.persistence.Store;
import com.kfood.merchant.infra.persistence.StoreBusinessHour;
import com.kfood.merchant.infra.persistence.StoreBusinessHourRepository;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreCheckoutRulesValidatorTest {

  private final StoreBusinessHourRepository storeBusinessHourRepository =
      mock(StoreBusinessHourRepository.class);

  @Test
  void shouldAcceptWhenInsideBusinessHours() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(
            List.of(
                StoreBusinessHour.open(
                    store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0))));

    assertThatCode(() -> validator.ensureStoreWithinBusinessHours(store))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptWhenStoreIsOperational() {
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, Clock.systemUTC());

    assertThatCode(() -> validator.ensureStoreOperational(activeStore()))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWhenOutsideBusinessHours() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(
            List.of(
                StoreBusinessHour.open(
                    store, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0))));

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  @Test
  void shouldRejectWhenCurrentTimeIsBeforeOpeningBoundary() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(
            List.of(
                StoreBusinessHour.open(
                    store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0))));

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  @Test
  void shouldRejectWhenStoreIsNotActive() {
    var store = suspendedStore();
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, Clock.systemUTC());

    assertThatThrownBy(() -> validator.ensureStoreOperational(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_NOT_ACTIVE);
  }

  @Test
  void shouldUseDefaultConstructorAndIgnoreClosedSlots() {
    var store = activeStore();
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(List.of(StoreBusinessHour.closed(store, DayOfWeek.MONDAY)));

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  @Test
  void shouldSkipClosedSlotInsideLoop() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(List.of(StoreBusinessHour.closed(store, DayOfWeek.MONDAY)));

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  @Test
  void shouldRejectWhenStoreHasNoBusinessHoursForCurrentDay() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(List.of());

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  @Test
  void shouldIgnoreOpenSlotsWithoutBoundariesWhenAnotherSlotMatches() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);
    var withoutOpenTime = mock(StoreBusinessHour.class);
    when(withoutOpenTime.isClosed()).thenReturn(false);
    when(withoutOpenTime.getOpenTime()).thenReturn(null);
    when(withoutOpenTime.getCloseTime()).thenReturn(LocalTime.of(22, 0));
    var withoutCloseTime = mock(StoreBusinessHour.class);
    when(withoutCloseTime.isClosed()).thenReturn(false);
    when(withoutCloseTime.getOpenTime()).thenReturn(LocalTime.of(10, 0));
    when(withoutCloseTime.getCloseTime()).thenReturn(null);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(
            List.of(
                withoutOpenTime,
                withoutCloseTime,
                StoreBusinessHour.open(
                    store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0))));

    assertThatCode(() -> validator.ensureStoreWithinBusinessHours(store))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectSlotsWithoutCompleteBoundaries() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-23T15:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);
    var withoutOpenTime = mock(StoreBusinessHour.class);
    when(withoutOpenTime.isClosed()).thenReturn(false);
    when(withoutOpenTime.getOpenTime()).thenReturn(null);
    when(withoutOpenTime.getCloseTime()).thenReturn(LocalTime.of(22, 0));
    var withoutCloseTime = mock(StoreBusinessHour.class);
    when(withoutCloseTime.isClosed()).thenReturn(false);
    when(withoutCloseTime.getOpenTime()).thenReturn(LocalTime.of(10, 0));
    when(withoutCloseTime.getCloseTime()).thenReturn(null);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(List.of(withoutOpenTime, withoutCloseTime));

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  @Test
  void shouldRejectWhenCurrentTimeMatchesClosingBoundary() {
    var store = activeStore();
    var clock = Clock.fixed(Instant.parse("2026-03-24T01:00:00Z"), ZoneId.of("UTC"));
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, clock);

    when(storeBusinessHourRepository.findAllByStoreIdAndDayOfWeek(store.getId(), DayOfWeek.MONDAY))
        .thenReturn(
            List.of(
                StoreBusinessHour.open(
                    store, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0))));

    assertThatThrownBy(() -> validator.ensureStoreWithinBusinessHours(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_OUTSIDE_BUSINESS_HOURS);
  }

  private Store activeStore() {
    var store =
        new Store(
            UUID.randomUUID(),
            "Loja do Bairro",
            "loja-do-bairro",
            "45.723.174/0001-10",
            "21999990000",
            "America/Sao_Paulo");
    store.activate();
    return store;
  }

  private Store suspendedStore() {
    var store = activeStore();
    store.suspend();
    return store;
  }
}
