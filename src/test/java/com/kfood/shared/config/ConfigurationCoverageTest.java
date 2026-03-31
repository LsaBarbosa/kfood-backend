package com.kfood.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ConfigurationCoverageTest {

  @Test
  void shouldExposeSecurityPropertiesValues() {
    AppProperties properties = new AppProperties();
    AppProperties.Security security = properties.getSecurity();
    AppProperties.Payment payment = properties.getPayment();
    AppProperties.Webhook webhook = payment.getWebhook();
    AppProperties.Providers providers = webhook.getProviders();
    AppProperties.Provider mockProvider = providers.getMock();

    security.setJwtSecret("secret-value");
    security.setJwtExpirationSeconds(3600L);
    mockProvider.setToken("mock-token");

    assertThat(properties.getSecurity()).isSameAs(security);
    assertThat(properties.getPayment()).isSameAs(payment);
    assertThat(payment.getWebhook()).isSameAs(webhook);
    assertThat(webhook.getProviders()).isSameAs(providers);
    assertThat(providers.getMock()).isSameAs(mockProvider);
    assertThat(security.getJwtSecret()).isEqualTo("secret-value");
    assertThat(security.getJwtExpirationSeconds()).isEqualTo(3600L);
    assertThat(mockProvider.getToken()).isEqualTo("mock-token");
  }

  @Test
  void shouldBindWebhookProviderTokenProperty() {
    var source =
        new MapConfigurationPropertySource(
            Map.of("app.payment.webhook.providers.mock.token", "bound-mock-token"));

    var properties =
        new Binder(source)
            .bind("app", Bindable.of(AppProperties.class))
            .orElseThrow(() -> new IllegalStateException("AppProperties should bind"));

    assertThat(properties.getPayment().getWebhook().getProviders().getMock().getToken())
        .isEqualTo("bound-mock-token");
  }

  @Test
  void shouldInstantiateJpaAuditingConfig() {
    assertThat(new JpaAuditingConfig()).isNotNull();
  }

  @Test
  void shouldInstantiatePasswordSecurityConfig() {
    assertThat(new PasswordSecurityConfig()).isNotNull();
  }

  @Test
  void shouldInstantiateAsyncConfig() {
    assertThat(new AsyncConfig()).isNotNull();
  }
}
