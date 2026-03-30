package com.kfood.merchant.app;

import java.util.List;

public record StoreHoursOutput(int hoursVersion, List<StoreHourOutput> hours) {}
