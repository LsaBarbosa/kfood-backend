package com.kfood.shared.tenancy;

import java.util.UUID;

public interface CurrentTenantProvider {

  UUID getRequiredStoreId();
}
