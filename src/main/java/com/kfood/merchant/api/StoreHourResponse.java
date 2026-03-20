package com.kfood.merchant.api;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record StoreHourResponse(
    DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean isClosed) {}
