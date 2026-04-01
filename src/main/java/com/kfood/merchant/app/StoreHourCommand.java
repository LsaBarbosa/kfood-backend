package com.kfood.merchant.app;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record StoreHourCommand(
    DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}
