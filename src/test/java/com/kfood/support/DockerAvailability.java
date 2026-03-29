package com.kfood.support;

import java.util.function.BooleanSupplier;
import org.testcontainers.DockerClientFactory;

final class DockerAvailability {

  private DockerAvailability() {}

  static boolean isDockerAvailable() {
    return isDockerAvailable(() -> DockerClientFactory.instance().isDockerAvailable());
  }

  static boolean isDockerAvailable(BooleanSupplier probe) {
    try {
      return probe.getAsBoolean();
    } catch (RuntimeException exception) {
      return false;
    }
  }
}
