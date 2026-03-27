package com.kfood.merchant.infra.user;

import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.app.AuthenticatedUserNotFoundException;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.TenantAccessDeniedException;
import com.kfood.merchant.application.user.port.MerchantTenantAccessPort;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  IdentityUserRepository.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class
})
public class MerchantTenantAccessAdapter implements MerchantTenantAccessPort {

  private final StoreRepository storeRepository;
  private final IdentityUserRepository identityUserRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

  public MerchantTenantAccessAdapter(
      StoreRepository storeRepository,
      IdentityUserRepository identityUserRepository,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider) {
    this.storeRepository = storeRepository;
    this.identityUserRepository = identityUserRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
  }

  @Override
  public UUID getRequiredStoreId() {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var authenticatedUserId = currentAuthenticatedUserProvider.getRequiredUserId();

    storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    var authenticatedUser =
        identityUserRepository
            .findById(authenticatedUserId)
            .orElseThrow(() -> new AuthenticatedUserNotFoundException(authenticatedUserId));

    if (!Objects.equals(authenticatedUser.getStoreId(), storeId)) {
      throw new TenantAccessDeniedException();
    }

    return storeId;
  }
}
