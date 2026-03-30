package com.kfood.merchant.app;

import java.util.List;

public record UpdateStoreHoursCommand(List<StoreHourCommand> hours) {}
