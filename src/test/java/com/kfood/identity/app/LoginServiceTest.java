package com.kfood.identity.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.shared.config.AppProperties;
import com.kfood.shared.exceptions.BusinessException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class LoginServiceTest {

  private final IdentityUserRepository identityUserRepository = mock(IdentityUserRepository.class);
  private final PasswordHashService passwordHashService = mock(PasswordHashService.class);
  private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
  private final AppProperties appProperties = appProperties();

  private final LoginService loginService =
      new LoginService(
          new ObjectProvider<>() {
            @Override
            public IdentityUserRepository getObject(Object... args) {
              return identityUserRepository;
            }

            @Override
            public IdentityUserRepository getIfAvailable() {
              return identityUserRepository;
            }

            @Override
            public IdentityUserRepository getIfUnique() {
              return identityUserRepository;
            }
          },
          passwordHashService,
          jwtTokenService,
          appProperties);

  @Test
  @DisplayName("should return token when credentials are valid")
  void shouldReturnTokenWhenCredentialsAreValid() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    when(identityUserRepository.findByEmail("owner@kfood.local")).thenReturn(Optional.of(user));
    when(passwordHashService.matches("Senha@123", "$2a$10$hash")).thenReturn(true);
    when(jwtTokenService.generateToken(user)).thenReturn("jwt-token");

    var response = loginService.login("owner@kfood.local", "Senha@123");

    assertThat(response.accessToken()).isEqualTo("jwt-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(3600);
    assertThat(response.user().email()).isEqualTo("owner@kfood.local");
    assertThat(response.user().roles()).containsExactly("OWNER");
  }

  @Test
  @DisplayName("should return unauthorized when password is invalid")
  void shouldReturnUnauthorizedWhenPasswordIsInvalid() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    when(identityUserRepository.findByEmail("owner@kfood.local")).thenReturn(Optional.of(user));
    when(passwordHashService.matches("senha-errada", "$2a$10$hash")).thenReturn(false);

    assertThatThrownBy(() -> loginService.login("owner@kfood.local", "senha-errada"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Invalid credentials.");
  }

  @Test
  @DisplayName("should return unauthorized when user does not exist")
  void shouldReturnUnauthorizedWhenUserDoesNotExist() {
    when(identityUserRepository.findByEmail("missing@kfood.local")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loginService.login("missing@kfood.local", "Senha@123"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Invalid credentials.");
  }

  @Test
  @DisplayName("should return locked when user is inactive")
  void shouldReturnLockedWhenUserIsInactive() {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "owner@kfood.local",
            "$2a$10$hash",
            UserStatus.INACTIVE);
    user.replaceRoles(Set.of(UserRoleName.OWNER));

    when(identityUserRepository.findByEmail("owner@kfood.local")).thenReturn(Optional.of(user));
    when(passwordHashService.matches("Senha@123", "$2a$10$hash")).thenReturn(true);

    assertThatThrownBy(() -> loginService.login("owner@kfood.local", "Senha@123"))
        .isInstanceOf(UserAuthenticationLockedException.class)
        .hasMessage("User is locked or inactive.");
  }

  private AppProperties appProperties() {
    var properties = new AppProperties();
    properties.getSecurity().setJwtSecret("12345678901234567890123456789012");
    properties.getSecurity().setJwtExpirationSeconds(3600);
    return properties;
  }
}
