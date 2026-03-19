package com.kfood.identity.infra.security;

import com.kfood.identity.app.PasswordHashService;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashServiceImpl implements PasswordHashService {

  private final PasswordEncoder passwordEncoder;

  public PasswordHashServiceImpl(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String hash(String rawPassword) {
    validateRawPassword(rawPassword);
    return passwordEncoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String passwordHash) {
    Objects.requireNonNull(rawPassword, "rawPassword is required");
    Objects.requireNonNull(passwordHash, "passwordHash is required");
    return passwordEncoder.matches(rawPassword, passwordHash);
  }

  private void validateRawPassword(String rawPassword) {
    Objects.requireNonNull(rawPassword, "rawPassword is required");

    if (rawPassword.isBlank()) {
      throw new IllegalArgumentException("rawPassword must not be blank");
    }

    if (rawPassword.length() < 8) {
      throw new IllegalArgumentException("rawPassword must have at least 8 characters");
    }
  }
}
