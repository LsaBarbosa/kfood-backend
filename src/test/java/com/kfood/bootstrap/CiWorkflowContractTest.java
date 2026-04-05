package com.kfood.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CiWorkflowContractTest {

  private static final Path CI_WORKFLOW_PATH = Path.of(".github/workflows/ci.yml");

  @Test
  void shouldContainCiWorkflowFile() {
    assertThat(CI_WORKFLOW_PATH).exists().isRegularFile();
  }

  @Test
  void shouldContainBuildTestFormattingAndCoverageSteps() throws IOException {
    var workflow = Files.readString(CI_WORKFLOW_PATH);

    assertThat(workflow).contains("jobs:");
    assertThat(workflow).contains("build:");
    assertThat(workflow).contains("spotlessCheck");
    assertThat(workflow).contains("clean test");
    assertThat(workflow).contains("jacocoTestReport");
    assertThat(workflow).contains("jacocoTestCoverageVerification");
  }
}
