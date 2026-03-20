package com.kfood.identity.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.shared.config.AppProperties;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class LoginServiceAdditionalTest {

  private final PasswordHashService passwordHashService = mock(PasswordHashService.class);
  private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);

  @Test
  void shouldThrowWhenRepositoryBeanIsMissing() {
    var service =
        new LoginService(
            new ObjectProvider<>() {
              @Override
              public IdentityUserRepository getObject(Object... args) {
                return null;
              }

              @Override
              public IdentityUserRepository getIfAvailable() {
                return null;
              }

              @Override
              public IdentityUserRepository getIfUnique() {
                return null;
              }
            },
            passwordHashService,
            jwtTokenService,
            appProperties());

    assertThatThrownBy(() -> service.login("missing@kfood.local", "Senha@123"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("IdentityUserRepository bean is required");
  }

  @Test
  void shouldReturnLockedWhenUserIsLocked() {
    var identityUserRepository = mock(IdentityUserRepository.class);
    var service =
        new LoginService(
            providerOf(identityUserRepository),
            passwordHashService,
            jwtTokenService,
            appProperties());
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.LOCKED);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    when(identityUserRepository.findByEmail("owner@kfood.local")).thenReturn(Optional.of(user));
    when(passwordHashService.matches("Senha@123", "$2a$10$hash")).thenReturn(true);

    assertThatThrownBy(() -> service.login("owner@kfood.local", "Senha@123"))
        .isInstanceOf(UserAuthenticationLockedException.class)
        .hasMessage("User is locked or inactive.");
  }

  private ObjectProvider<IdentityUserRepository> providerOf(IdentityUserRepository repository) {
    return new ObjectProvider<>() {
      @Override
      public IdentityUserRepository getObject(Object... args) {
        return repository;
      }

      @Override
      public IdentityUserRepository getIfAvailable() {
        return repository;
      }

      @Override
      public IdentityUserRepository getIfUnique() {
        return repository;
      }
    };
  }

  private AppProperties appProperties() {
    var properties = new AppProperties();
    properties.getSecurity().setJwtSecret("12345678901234567890123456789012");
    properties.getSecurity().setJwtExpirationSeconds(3600);
    return properties;
  }
}
