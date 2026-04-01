package com.kfood.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

class DockerAvailabilityCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (DockerAvailability.isDockerAvailable()) {
      return ConditionEvaluationResult.enabled("Docker is available.");
    }
    return ConditionEvaluationResult.disabled("Docker is not available.");
  }
}
