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
  void shouldRejectWhenStoreIsNotActive() {
    var store = suspendedStore();
    var validator = new StoreCheckoutRulesValidator(storeBusinessHourRepository, Clock.systemUTC());

    assertThatThrownBy(() -> validator.ensureStoreOperational(store))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_NOT_ACTIVE);
  }

  private Store activeStore() {
    return new Store(
        UUID.randomUUID(),
        "Loja do Bairro",
        "loja-do-bairro",
        "45.723.174/0001-10",
        "21999990000",
        "America/Sao_Paulo");
  }

  private Store suspendedStore() {
    var store = activeStore();
    store.activate();
    store.suspend();
    return store;
  }
}
