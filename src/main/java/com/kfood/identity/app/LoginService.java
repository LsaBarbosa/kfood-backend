package com.kfood.identity.app;

import com.kfood.identity.api.LoginResponse;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.shared.config.AppProperties;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

  private final IdentityUserRepository identityUserRepository;
  private final PasswordHashService passwordHashService;
  private final JwtTokenService jwtTokenService;
  private final AppProperties appProperties;

  public LoginService(
      ObjectProvider<IdentityUserRepository> identityUserRepositoryProvider,
      PasswordHashService passwordHashService,
      JwtTokenService jwtTokenService,
      AppProperties appProperties) {
    identityUserRepository = identityUserRepositoryProvider.getIfAvailable();
    this.passwordHashService = passwordHashService;
    this.jwtTokenService = jwtTokenService;
    this.appProperties = appProperties;
  }

  @Transactional(readOnly = true)
  public LoginResponse login(String email, String rawPassword) {
    var user = repository().findByEmail(email).orElseThrow(this::invalidCredentialsException);

    if (!passwordHashService.matches(rawPassword, user.getPasswordHash())) {
      throw invalidCredentialsException();
    }

    if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.INACTIVE) {
      throw new UserAuthenticationLockedException("User is locked or inactive.");
    }

    var token = jwtTokenService.generateToken(user);
    var roles =
        user.getRoles().stream().map(role -> role.getRoleName().name()).collect(Collectors.toSet());

    return new LoginResponse(
        token,
        "Bearer",
        appProperties.getSecurity().getJwtExpirationSeconds(),
        new LoginResponse.AuthenticatedUserResponse(
            user.getId(), user.getEmail(), roles, user.getStoreId(), user.getStatus().name()));
  }

  private BusinessException invalidCredentialsException() {
    return new BusinessException(
        ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials.", HttpStatus.UNAUTHORIZED);
  }

  private IdentityUserRepository repository() {
    if (identityUserRepository == null) {
      throw new IllegalStateException("IdentityUserRepository bean is required");
    }
    return identityUserRepository;
  }
}
