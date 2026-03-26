package com.kfood.merchant.app;

import com.kfood.identity.app.CreateUserService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.api.CreateMerchantUserRequest;
import com.kfood.merchant.api.MerchantUserResponse;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  CreateUserService.class,
  StoreRepository.class,
  IdentityUserRepository.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class
})
public class CreateMerchantUserUseCase {

  private static final Set<UserRoleName> ALLOWED_ROLES =
      EnumSet.of(UserRoleName.MANAGER, UserRoleName.ATTENDANT);

  private final CreateUserService createUserService;
  private final MerchantTenantAccessService merchantTenantAccessService;

  public CreateMerchantUserUseCase(
      CreateUserService createUserService, MerchantTenantAccessService merchantTenantAccessService) {
    this.createUserService = createUserService;
    this.merchantTenantAccessService = merchantTenantAccessService;
  }

  @Transactional
  public MerchantUserResponse execute(CreateMerchantUserRequest request) {
    validateRoles(request.roles());

    var storeId = merchantTenantAccessService.getRequiredStoreId();
    var created = createUserService.create(storeId, request.email().trim(), request.password(), request.roles());

    return MerchantUserMapper.toResponse(created);
  }

  private void validateRoles(Set<UserRoleName> roles) {
    for (var role : roles) {
      if (!ALLOWED_ROLES.contains(role)) {
        throw new InvalidMerchantUserRoleException(role);
      }
    }
  }
}
