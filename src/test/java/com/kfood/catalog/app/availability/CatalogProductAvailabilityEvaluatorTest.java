package com.kfood.catalog.app.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.catalog.infra.persistence.CatalogCategory;
import com.kfood.catalog.infra.persistence.CatalogProduct;
import com.kfood.catalog.infra.persistence.CatalogProductAvailabilityWindow;
import com.kfood.merchant.infra.persistence.Store;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogProductAvailabilityEvaluatorTest {

  @Test
  void shouldAllowProductInsideWindow() {
    var product = product(true, false);
    product.replaceAvailabilityWindows(
        List.of(
            new CatalogProductAvailabilityWindow(
                UUID.randomUUID(),
                product,
                DayOfWeek.MONDAY,
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                true)));

    var evaluator = new CatalogProductAvailabilityEvaluator();

    var result = evaluator.isAvailableAt(product, ZonedDateTime.parse("2026-03-23T12:00:00-03:00"));

    assertThat(result).isTrue();
  }

  @Test
  void shouldBlockProductOutsideWindow() {
    var product = product(true, false);
    product.replaceAvailabilityWindows(
        List.of(
            new CatalogProductAvailabilityWindow(
                UUID.randomUUID(),
                product,
                DayOfWeek.MONDAY,
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                true)));

    var evaluator = new CatalogProductAvailabilityEvaluator();

    var result = evaluator.isAvailableAt(product, ZonedDateTime.parse("2026-03-23T16:00:00-03:00"));

    assertThat(result).isFalse();
  }

  @Test
  void shouldAllowProductWithoutAvailabilityWindows() {
    var product = product(true, false);

    var evaluator = new CatalogProductAvailabilityEvaluator();

    var result = evaluator.isAvailableAt(product, ZonedDateTime.parse("2026-03-23T16:00:00-03:00"));

    assertThat(result).isTrue();
  }

  @Test
  void shouldBlockInactiveOrPausedProduct() {
    var inactiveProduct = product(false, false);
    var pausedProduct = product(true, true);
    var evaluator = new CatalogProductAvailabilityEvaluator();
    var dateTime = ZonedDateTime.parse("2026-03-23T12:00:00-03:00");

    assertThat(evaluator.isAvailableAt(inactiveProduct, dateTime)).isFalse();
    assertThat(evaluator.isAvailableAt(pausedProduct, dateTime)).isFalse();
  }

  @Test
  void shouldEvaluateUsingStoreTimezone() {
    var product = product(true, false);
    product.replaceAvailabilityWindows(
        List.of(
            new CatalogProductAvailabilityWindow(
                UUID.randomUUID(),
                product,
                DayOfWeek.MONDAY,
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                true)));

    var evaluator =
        new CatalogProductAvailabilityEvaluator(
            Clock.fixed(Instant.parse("2026-03-23T15:30:00Z"), ZoneId.of("UTC")));

    var result = evaluator.isAvailableNow(product, ZoneId.of("America/Sao_Paulo"));

    assertThat(result).isTrue();
  }

  private CatalogProduct product(boolean active, boolean paused) {
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
        active,
        paused);
  }
}
