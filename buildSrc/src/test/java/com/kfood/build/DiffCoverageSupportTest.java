package com.kfood.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiffCoverageSupportTest {

  @Test
  void shouldReturnChangedMainClassesWhenPrimaryDiffSucceeds() {
    var commands = new java.util.ArrayList<List<String>>();
    DiffCoverageSupport.CommandRunner runner =
        (workingDirectory, command) -> {
          commands.add(command);
          return new DiffCoverageSupport.CommandResult(
              0,
              """
              src/main/java/com/kfood/payment/app/CreateOrderPixPaymentUseCase.java
              src/main/java/com/kfood/order/api/OrderController.java
              src/test/java/com/kfood/payment/app/CreateOrderPixPaymentUseCaseTest.java
              """,
              "");
        };

    var changedClasses =
        DiffCoverageSupport.changedMainClasses(new File("."), "origin/main", runner);

    assertThat(changedClasses)
        .containsExactly(
            "com.kfood.payment.app.CreateOrderPixPaymentUseCase",
            "com.kfood.order.api.OrderController");
    assertThat(commands).hasSize(1);
    assertThat(commands.getFirst()).contains("origin/main...HEAD");
  }

  @Test
  void shouldFallbackToTwoDotDiffWhenPrimaryAttemptFails() {
    var invocationCount = new AtomicInteger();
    DiffCoverageSupport.CommandRunner runner =
        (workingDirectory, command) -> {
          if (invocationCount.getAndIncrement() == 0) {
            return new DiffCoverageSupport.CommandResult(128, "", "unknown revision");
          }
          return new DiffCoverageSupport.CommandResult(
              0, "src/main/java/com/kfood/shared/config/SecurityConfiguration.java\n", "");
        };

    var changedClasses =
        DiffCoverageSupport.changedMainClasses(new File("."), "origin/main", runner);

    assertThat(changedClasses).containsExactly("com.kfood.shared.config.SecurityConfiguration");
    assertThat(invocationCount).hasValue(2);
  }

  @Test
  void shouldFailClosedWhenAllDiffAttemptsFail() {
    DiffCoverageSupport.CommandRunner runner =
        (workingDirectory, command) ->
            new DiffCoverageSupport.CommandResult(128, "", "fatal: bad revision");

    assertThatThrownBy(
            () -> DiffCoverageSupport.changedMainClasses(new File("."), "origin/main", runner))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to calculate changed main classes from git diff.")
        .hasMessageContaining("origin/main...HEAD")
        .hasMessageContaining("origin/main HEAD")
        .hasMessageContaining("fatal: bad revision");
  }

  @Test
  void shouldDeriveChangedMainClassesFromDiffOutput() {
    var changedClasses =
        DiffCoverageSupport.deriveChangedMainClasses(
            """
            src/main/java/com/kfood/payment/app/CreatePixChargeUseCase.java
            src/main/java/com/kfood/payment/app/gateway/PixChargeGatewayRegistry.java
            src/main/java/com/kfood/payment/app/package-info.java
            src/main/resources/application.yml
            src/test/java/com/kfood/payment/app/CreatePixChargeUseCaseTest.java
            """);

    assertThat(changedClasses)
        .containsExactly(
            "com.kfood.payment.app.CreatePixChargeUseCase",
            "com.kfood.payment.app.gateway.PixChargeGatewayRegistry");
  }

  @Test
  void shouldReturnEmptySetWhenDiffHasNoMainJavaChanges() {
    var changedClasses =
        DiffCoverageSupport.deriveChangedMainClasses(
            """
            README.md
            src/test/java/com/kfood/payment/app/CreatePixChargeUseCaseTest.java
            src/main/resources/db/migration/V99__example.sql
            """);

    assertThat(changedClasses).isEmpty();
  }

  @Test
  void shouldExcludeInterfacesFromChangedClassCoverageEnforcement(@TempDir java.nio.file.Path tempDir)
      throws Exception {
    var sourceRoot = tempDir.resolve("src/main/java/com/kfood/payment/app/port");
    Files.createDirectories(sourceRoot);
    Files.writeString(
        sourceRoot.resolve("PaymentPersistencePort.java"),
        """
        package com.kfood.payment.app.port;

        public interface PaymentPersistencePort {}
        """);

    var changedClasses =
        DiffCoverageSupport.changedMainClasses(
            tempDir.toFile(),
            "origin/main",
            (workingDirectory, command) ->
                new DiffCoverageSupport.CommandResult(
                    0,
                    "src/main/java/com/kfood/payment/app/port/PaymentPersistencePort.java\n",
                    ""));

    assertThat(changedClasses).isEmpty();
  }

  @Test
  void shouldKeepConcreteClassesInChangedCoverageEnforcement(@TempDir java.nio.file.Path tempDir)
      throws Exception {
    var sourceRoot = tempDir.resolve("src/main/java/com/kfood/order/api");
    Files.createDirectories(sourceRoot);
    Files.writeString(
        sourceRoot.resolve("OrderController.java"),
        """
        package com.kfood.order.api;

        public class OrderController {}
        """);

    var changedClasses =
        DiffCoverageSupport.changedMainClasses(
            tempDir.toFile(),
            "origin/main",
            (workingDirectory, command) ->
                new DiffCoverageSupport.CommandResult(
                    0, "src/main/java/com/kfood/order/api/OrderController.java\n", ""));

    assertThat(changedClasses).containsExactly("com.kfood.order.api.OrderController");
  }

  @Test
  void shouldReturnFalseWhenCoverageShouldNotBeEnforcedForAnnotationInterface(
      @TempDir java.nio.file.Path tempDir) throws Exception {
    var sourceRoot = tempDir.resolve("src/main/java/com/kfood/shared");
    Files.createDirectories(sourceRoot);
    Files.writeString(
        sourceRoot.resolve("ExampleAnnotation.java"),
        """
        package com.kfood.shared;

        public @interface ExampleAnnotation {}
        """);

    assertThat(
            DiffCoverageSupport.shouldEnforceCoverage(
                tempDir.toFile(), "com.kfood.shared.ExampleAnnotation"))
        .isFalse();
  }
}
