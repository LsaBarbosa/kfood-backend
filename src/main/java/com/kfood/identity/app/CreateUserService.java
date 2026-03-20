package com.kfood.identity.app;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CreateUserService {

  private final IdentityUserRepository identityUserRepository;
  private final PasswordHashService passwordHashService;

  public CreateUserService(
      IdentityUserRepository identityUserRepository, PasswordHashService passwordHashService) {
    this.identityUserRepository = identityUserRepository;
    this.passwordHashService = passwordHashService;
  }

  @Transactional
  public IdentityUserEntity create(
      UUID storeId, String email, String rawPassword, Set<UserRoleName> roles) {
    String passwordHash = passwordHashService.hash(rawPassword);

    IdentityUserEntity user =
        new IdentityUserEntity(UUID.randomUUID(), storeId, email, passwordHash, UserStatus.ACTIVE);
    user.replaceRoles(roles);

    return identityUserRepository.save(user);
  }
}
