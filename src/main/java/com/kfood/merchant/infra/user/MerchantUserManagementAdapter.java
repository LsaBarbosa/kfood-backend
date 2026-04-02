package com.kfood.merchant.infra.user;

import com.kfood.identity.app.CreateUserService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.application.user.MerchantUserOutput;
import com.kfood.merchant.application.user.port.MerchantUserManagementPort;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MerchantUserManagementAdapter implements MerchantUserManagementPort {

  private final CreateUserService createUserService;
  private final IdentityUserRepository identityUserRepository;

  public MerchantUserManagementAdapter(
      CreateUserService createUserService, IdentityUserRepository identityUserRepository) {
    this.createUserService = createUserService;
    this.identityUserRepository = identityUserRepository;
  }

  @Override
  public MerchantUserOutput create(
      UUID storeId, String email, String rawPassword, Set<UserRoleName> roles, UserStatus status) {
    return toOutput(createUserService.create(storeId, email, rawPassword, roles, status));
  }

  @Override
  public List<MerchantUserOutput> listByStoreId(UUID storeId) {
    return identityUserRepository.findAllByStoreIdOrderByEmailAsc(storeId).stream()
        .map(MerchantUserManagementAdapter::toOutput)
        .toList();
  }

  static MerchantUserOutput toOutput(IdentityUserEntity user) {
    return new MerchantUserOutput(
        user.getId(),
        user.getEmail(),
        user.getRoles().stream()
            .map(role -> role.getRoleName().name())
            .sorted(Comparator.naturalOrder())
            .toList(),
        user.getStatus(),
        user.getCreatedAt());
  }
}
