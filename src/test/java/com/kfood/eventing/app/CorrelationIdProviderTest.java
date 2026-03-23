package com.kfood.eventing.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationIdProviderTest {

  private final CorrelationIdProvider provider = new CorrelationIdProvider();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void shouldReturnTraceIdFromMdcWhenPresent() {
    MDC.put("traceId", "trace-123");

    assertThat(provider.getOrCreate()).isEqualTo("trace-123");
  }

  @Test
  void shouldGenerateCorrelationIdWhenTraceIdIsMissing() {
    assertThat(provider.getOrCreate()).isNotBlank();
  }
}
