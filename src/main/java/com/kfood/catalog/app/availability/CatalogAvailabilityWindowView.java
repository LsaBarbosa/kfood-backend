package com.kfood.catalog.app.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public interface CatalogAvailabilityWindowView {

  boolean isActive();

  boolean matches(DayOfWeek dayOfWeek, LocalTime localTime);
}
