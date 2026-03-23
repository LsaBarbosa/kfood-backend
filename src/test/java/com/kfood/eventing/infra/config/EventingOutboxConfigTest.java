package com.kfood.eventing.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EventingOutboxConfigTest {

  @Test
  void shouldInstantiateEventingOutboxConfig() {
    assertThat(new EventingOutboxConfig()).isNotNull();
  }

  @Test
  void shouldExposeEventingOutboxPropertiesValues() {
    var properties = new EventingOutboxProperties(25, 3000);
    var config = new EventingOutboxConfig();

    assertThat(properties.batchSize()).isEqualTo(25);
    assertThat(properties.retryInterval()).isEqualTo(3000);
    assertThat(config.objectMapper()).isInstanceOf(ObjectMapper.class);
  }
}
