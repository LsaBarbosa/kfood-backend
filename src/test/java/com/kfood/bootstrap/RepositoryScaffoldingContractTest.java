package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryScaffoldingContractTest {

  private static final List<Path> REQUIRED_PROJECT_FILES =
      List.of(Path.of("README.md"), Path.of(".gitignore"), Path.of(".editorconfig"));

  @Test
  void shouldContainRequiredRepositoryBaseFiles() {
    assertThat(REQUIRED_PROJECT_FILES).allSatisfy(path -> assertThat(path).exists().isRegularFile());
  }
}
