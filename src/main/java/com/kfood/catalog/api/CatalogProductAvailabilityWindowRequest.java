package com.kfood.catalog.api;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record CatalogProductAvailabilityWindowRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    Boolean active) {}
