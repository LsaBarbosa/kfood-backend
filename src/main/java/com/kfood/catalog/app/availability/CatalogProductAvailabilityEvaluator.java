package com.kfood.catalog.app.availability;

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

  public boolean isAvailableNow(CatalogProductAvailabilityView product, ZoneId storeZoneId) {
    var now = ZonedDateTime.now(clock).withZoneSameInstant(storeZoneId);
    return isAvailableAt(product, now);
  }

  public boolean isAvailableAt(CatalogProductAvailabilityView product, ZonedDateTime dateTime) {
    if (!product.isActive() || product.isPaused()) {
      return false;
    }

    var activeWindows =
        product.getAvailabilityWindows().stream()
            .filter(CatalogAvailabilityWindowView::isActive)
            .toList();

    if (activeWindows.isEmpty()) {
      return true;
    }

    var dayOfWeek = dateTime.getDayOfWeek();
    var localTime = dateTime.toLocalTime();

    return activeWindows.stream().anyMatch(window -> window.matches(dayOfWeek, localTime));
  }
}
