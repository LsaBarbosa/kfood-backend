package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectProfilesContractTest {

  private static final List<Path> REQUIRED_PROFILE_FILES =
      List.of(
          Path.of("src/main/resources/application-local.yml"),
          Path.of("src/main/resources/application-test.yml"),
          Path.of("src/main/resources/application-prod.yml"));

  @Test
  void shouldContainRequiredProfileFiles() {
    assertThat(REQUIRED_PROFILE_FILES).allSatisfy(path -> assertThat(path).exists().isRegularFile());
  }
}
