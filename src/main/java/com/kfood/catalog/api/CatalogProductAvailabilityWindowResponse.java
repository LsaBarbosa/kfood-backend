package com.kfood.catalog.api;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record CatalogProductAvailabilityWindowResponse(
    UUID id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, boolean active) {}
