package com.kfood.merchant.api;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record PublicStoreHourResponse(
    DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}
