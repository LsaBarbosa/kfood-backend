package com.kfood.merchant.app;

import com.kfood.merchant.domain.StoreStatus;

public record ChangeStoreStatusCommand(StoreStatus targetStatus) {}
