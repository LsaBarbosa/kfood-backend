package com.kfood.merchant.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.domain.UserStatus;
import com.kfood.merchant.application.user.MerchantUserOutput;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MerchantUserApiMapperTest {

  @Test
  void shouldMapApplicationOutputToApiResponse() {
    var output =
        new MerchantUserOutput(
            UUID.randomUUID(),
            "manager@kfood.local",
            List.of("MANAGER"),
            UserStatus.ACTIVE,
            Instant.parse("2026-03-26T12:00:00Z"));

    var response = MerchantUserApiMapper.toResponse(output);

    assertThat(response.id()).isEqualTo(output.id());
    assertThat(response.email()).isEqualTo("manager@kfood.local");
    assertThat(response.roles()).containsExactly("MANAGER");
    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
  }
}
