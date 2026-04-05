package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BuildQualityContractTest {

  private static final Path BUILD_GRADLE_PATH = Path.of("build.gradle");

  @Test
  void shouldContainSpotlessAndJacocoConfiguration() throws IOException {
    var buildGradle = Files.readString(BUILD_GRADLE_PATH);

    assertThat(buildGradle).contains("com.diffplug.spotless");
    assertThat(buildGradle).contains("tasks.named('jacocoTestReport')");
    assertThat(buildGradle).contains("tasks.named('jacocoTestCoverageVerification')");
  }

  @Test
  void shouldIntegrateQualityTasksIntoCheckLifecycle() throws IOException {
    var buildGradle = Files.readString(BUILD_GRADLE_PATH);

    assertThat(buildGradle).contains("tasks.named('check')");
    assertThat(buildGradle).contains("dependsOn tasks.named('spotlessCheck')");
    assertThat(buildGradle).contains("dependsOn tasks.named('jacocoTestReport')");
    assertThat(buildGradle).contains("dependsOn tasks.named('jacocoTestCoverageVerification')");
  }
}
