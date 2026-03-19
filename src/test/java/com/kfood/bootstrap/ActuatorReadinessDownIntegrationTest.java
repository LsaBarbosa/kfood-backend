package com.kfood.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "management.endpoint.health.probes.enabled=true",
      "management.endpoint.health.show-details=never",
      "management.endpoint.health.show-components=never",
      "management.endpoint.health.validate-group-membership=false",
      "management.endpoint.health.group.readiness.include=readinessState,db,redis,rabbit,testCriticalDependency"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorReadinessDownIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnReadinessDownWhenCriticalDependencyFails() throws Exception {
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @TestConfiguration
  static class TestHealthConfig {

    @Bean(name = "testCriticalDependency")
    @Primary
    HealthIndicator testCriticalDependencyHealthIndicator() {
      return () ->
          Health.down().withDetail("reason", "Simulated critical dependency failure").build();
    }
  }
}
