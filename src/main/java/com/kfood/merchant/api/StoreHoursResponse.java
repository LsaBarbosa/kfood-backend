package com.kfood.merchant.api;

import java.util.List;

public record StoreHoursResponse(int hoursVersion, List<StoreHourResponse> hours) {}
