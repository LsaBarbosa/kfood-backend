package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.identity.app.PasswordHashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashServiceImplTest {

  private final PasswordHashService passwordHashService =
      new PasswordHashServiceImpl(new BCryptPasswordEncoder());

  @Test
  @DisplayName("should generate hash different from raw password")
  void shouldGenerateHashDifferentFromRawPassword() {
    String rawPassword = "Senha@123";

    String hash = passwordHashService.hash(rawPassword);

    assertThat(hash).isNotBlank();
    assertThat(hash).isNotEqualTo(rawPassword);
  }

  @Test
  @DisplayName("should match raw password against generated hash")
  void shouldMatchRawPasswordAgainstGeneratedHash() {
    String rawPassword = "Senha@123";

    String hash = passwordHashService.hash(rawPassword);

    assertThat(passwordHashService.matches(rawPassword, hash)).isTrue();
  }

  @Test
  @DisplayName("should not match different password against generated hash")
  void shouldNotMatchDifferentPasswordAgainstGeneratedHash() {
    String hash = passwordHashService.hash("Senha@123");

    assertThat(passwordHashService.matches("Outra@123", hash)).isFalse();
  }

  @Test
  @DisplayName("should reject blank password")
  void shouldRejectBlankPassword() {
    assertThatThrownBy(() -> passwordHashService.hash("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rawPassword must not be blank");
  }

  @Test
  @DisplayName("should reject short password")
  void shouldRejectShortPassword() {
    assertThatThrownBy(() -> passwordHashService.hash("1234567"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rawPassword must have at least 8 characters");
  }
}
