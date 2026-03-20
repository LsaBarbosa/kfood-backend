package com.kfood.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigurationCoverageTest {

  @Test
  void shouldExposeSecurityPropertiesValues() {
    AppProperties properties = new AppProperties();
    AppProperties.Security security = properties.getSecurity();

    security.setJwtSecret("secret-value");
    security.setJwtExpirationSeconds(3600L);

    assertThat(properties.getSecurity()).isSameAs(security);
    assertThat(security.getJwtSecret()).isEqualTo("secret-value");
    assertThat(security.getJwtExpirationSeconds()).isEqualTo(3600L);
  }

  @Test
  void shouldInstantiateJpaAuditingConfig() {
    assertThat(new JpaAuditingConfig()).isNotNull();
  }

  @Test
  void shouldInstantiatePasswordSecurityConfig() {
    assertThat(new PasswordSecurityConfig()).isNotNull();
  }
}
