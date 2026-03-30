package com.kfood.merchant.app;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record PublicStoreHourOutput(
    DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}
