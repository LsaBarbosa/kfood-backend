package com.kfood.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DiffCoverageSupport {

  private static final String MAIN_JAVA_SOURCE_ROOT = "src/main/java/";

  private DiffCoverageSupport() {}

  @FunctionalInterface
  public interface CommandRunner {
    CommandResult run(File workingDirectory, List<String> command);
  }

  public record CommandResult(int exitCode, String stdout, String stderr) {}

  public static List<String> changedMainClasses(
      File rootDir, String diffBase, CommandRunner commandRunner) {
    Objects.requireNonNull(rootDir, "rootDir must not be null");
    var normalizedDiffBase =
        Objects.requireNonNull(diffBase, "diffBase must not be null").trim();
    if (normalizedDiffBase.isEmpty()) {
      throw new IllegalArgumentException("diffBase must not be blank");
    }
    var validatedRunner = Objects.requireNonNull(commandRunner, "commandRunner must not be null");

    var commands = diffCommands(normalizedDiffBase);
    var failures = new ArrayList<String>();

    for (var command : commands) {
      var result = validatedRunner.run(rootDir, command);
      if (result.exitCode() == 0) {
        return deriveChangedMainClasses(result.stdout());
      }

      failures.add(formatFailure(command, result));
    }

    throw new IllegalStateException(
        "Failed to calculate changed main classes from git diff. " + String.join(" | ", failures));
  }

  public static CommandRunner processBuilderRunner() {
    return (workingDirectory, command) -> {
      try {
        var process =
            new ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(false)
                .start();

        var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        var exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout, stderr);
      } catch (IOException exception) {
        throw new IllegalStateException(
            "Failed to execute git diff command: " + String.join(" ", command), exception);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupted while executing git diff command: " + String.join(" ", command),
            exception);
      }
    };
  }

  static List<String> deriveChangedMainClasses(String diffOutput) {
    Objects.requireNonNull(diffOutput, "diffOutput must not be null");

    Set<String> changedClasses = new LinkedHashSet<>();
    for (var rawLine : diffOutput.lines().toList()) {
      var normalizedPath = normalizeGitPath(rawLine);
      if (normalizedPath == null || !normalizedPath.startsWith(MAIN_JAVA_SOURCE_ROOT)) {
        continue;
      }
      if (!normalizedPath.endsWith(".java")
          || normalizedPath.endsWith("/package-info.java")
          || normalizedPath.endsWith("/module-info.java")) {
        continue;
      }

      var relativeClassPath =
          normalizedPath
              .substring(MAIN_JAVA_SOURCE_ROOT.length(), normalizedPath.length() - ".java".length())
              .replace('/', '.');
      if (!relativeClassPath.isBlank()) {
        changedClasses.add(relativeClassPath);
      }
    }
    return List.copyOf(changedClasses);
  }

  private static List<List<String>> diffCommands(String diffBase) {
    return List.of(
        List.of(
            "git",
            "diff",
            "--name-only",
            "--diff-filter=ACMR",
            diffBase + "...HEAD",
            "--",
            Path.of("src", "main", "java").toString().replace(File.separatorChar, '/')),
        List.of(
            "git",
            "diff",
            "--name-only",
            "--diff-filter=ACMR",
            diffBase,
            "HEAD",
            "--",
            Path.of("src", "main", "java").toString().replace(File.separatorChar, '/')));
  }

  private static String normalizeGitPath(String rawLine) {
    if (rawLine == null) {
      return null;
    }
    var trimmed = rawLine.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.replace('\\', '/');
  }

  private static String formatFailure(List<String> command, CommandResult result) {
    return "command='"
        + String.join(" ", command)
        + "', exitCode="
        + result.exitCode()
        + ", stderr='"
        + sanitizeForMessage(result.stderr())
        + "'";
  }

  private static String sanitizeForMessage(String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("\\s+", " ").trim();
  }
}
