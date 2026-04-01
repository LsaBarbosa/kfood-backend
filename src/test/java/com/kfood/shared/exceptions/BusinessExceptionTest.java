package com.kfood.shared.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

  @Test
  void shouldReplaceNullDetailsWithEmptyList() {
    BusinessException exception =
        new BusinessException(
            ErrorCode.STORE_NOT_ACTIVE, "Store is not active.", HttpStatus.CONFLICT, null);

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_ACTIVE);
    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception.getMessage()).isEqualTo("Store is not active.");
    assertThat(exception.getDetails()).isEmpty();
  }
}
