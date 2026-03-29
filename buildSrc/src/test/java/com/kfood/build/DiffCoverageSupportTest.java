package com.kfood.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

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
}
