package com.kfood.merchant.app;

import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.api.MerchantUserResponse;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  IdentityUserRepository.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class
})
public class ListMerchantUsersUseCase {

  private final IdentityUserRepository identityUserRepository;
  private final MerchantTenantAccessService merchantTenantAccessService;

  public ListMerchantUsersUseCase(
      IdentityUserRepository identityUserRepository,
      MerchantTenantAccessService merchantTenantAccessService) {
    this.identityUserRepository = identityUserRepository;
    this.merchantTenantAccessService = merchantTenantAccessService;
  }

  @Transactional(readOnly = true)
  public List<MerchantUserResponse> execute() {
    var storeId = merchantTenantAccessService.getRequiredStoreId();

    return identityUserRepository.findAllByStoreIdOrderByEmailAsc(storeId).stream()
        .map(MerchantUserMapper::toResponse)
        .toList();
  }
}
