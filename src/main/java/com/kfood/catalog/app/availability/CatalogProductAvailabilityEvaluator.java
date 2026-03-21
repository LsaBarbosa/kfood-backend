package com.kfood.catalog.app.availability;

import com.kfood.catalog.infra.persistence.CatalogProduct;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CatalogProductAvailabilityEvaluator {

  private final Clock clock;

  public CatalogProductAvailabilityEvaluator() {
    this(Clock.systemUTC());
  }

  public CatalogProductAvailabilityEvaluator(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock is required");
  }

  public boolean isAvailableNow(CatalogProduct product, ZoneId storeZoneId) {
    var now = ZonedDateTime.now(clock).withZoneSameInstant(storeZoneId);
    return isAvailableAt(product, now);
  }

  public boolean isAvailableAt(CatalogProduct product, ZonedDateTime dateTime) {
    if (!product.isActive() || product.isPaused()) {
      return false;
    }

    var activeWindows =
        product.getAvailabilityWindows().stream()
            .filter(com.kfood.catalog.infra.persistence.CatalogProductAvailabilityWindow::isActive)
            .toList();

    if (activeWindows.isEmpty()) {
      return true;
    }

    var dayOfWeek = dateTime.getDayOfWeek();
    var localTime = dateTime.toLocalTime();

    return activeWindows.stream().anyMatch(window -> window.matches(dayOfWeek, localTime));
  }
}
