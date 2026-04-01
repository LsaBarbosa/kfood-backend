package com.kfood.merchant.application.user.port;

import java.util.UUID;

public interface MerchantTenantAccessPort {

  UUID getRequiredStoreId();
}
