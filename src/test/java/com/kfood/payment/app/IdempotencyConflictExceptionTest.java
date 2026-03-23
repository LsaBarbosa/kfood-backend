package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.shared.exceptions.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class IdempotencyConflictExceptionTest {

  @Test
  void shouldExposeConflictMetadata() {
    var exception = new IdempotencyConflictException("conflict");

    assertThat(exception.getErrorCode())
        .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception).hasMessage("conflict");
  }
}
