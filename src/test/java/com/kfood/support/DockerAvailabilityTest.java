package com.kfood.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DockerAvailabilityTest {

  @Test
  void shouldReturnTrueWhenProbeReportsAvailable() {
    assertThat(DockerAvailability.isDockerAvailable(() -> true)).isTrue();
  }

  @Test
  void shouldReturnFalseWhenProbeReportsUnavailable() {
    assertThat(DockerAvailability.isDockerAvailable(() -> false)).isFalse();
  }

  @Test
  void shouldReturnFalseWhenProbeThrowsRuntimeException() {
    assertThat(
            DockerAvailability.isDockerAvailable(
                () -> {
                  throw new IllegalStateException("docker lookup failed");
                }))
        .isFalse();
  }
}
