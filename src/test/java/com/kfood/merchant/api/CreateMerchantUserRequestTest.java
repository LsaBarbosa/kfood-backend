package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CreateMerchantUserRequestTest {

  @Test
  void shouldExposeTemporaryPasswordRolesAndStatusWithoutPasswordField() {
    assertThat(
            Arrays.stream(CreateMerchantUserRequest.class.getRecordComponents())
                .map(RecordComponent::getName))
        .containsExactly("email", "temporaryPassword", "roles", "status")
        .doesNotContain("password");
  }
}
