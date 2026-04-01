package com.kfood.shared.security;

import java.util.UUID;

public interface TenantAwarePrincipal {

  UUID getStoreId();
}
