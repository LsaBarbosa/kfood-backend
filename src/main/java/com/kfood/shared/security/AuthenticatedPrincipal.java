package com.kfood.shared.security;

import java.util.UUID;

public interface AuthenticatedPrincipal extends TenantAwarePrincipal {

  UUID getUserId();
}
