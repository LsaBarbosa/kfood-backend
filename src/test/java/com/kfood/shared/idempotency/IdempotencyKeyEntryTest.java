package com.kfood.shared.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyKeyEntryTest {

  @Test
  void shouldCreateAndCompleteIdempotencyEntry() {
    var expiresAt = OffsetDateTime.now().plusDays(1);
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            UUID.randomUUID(),
            " public-order-create ",
            " idem-123 ",
            "request-hash",
            expiresAt);

    entry.complete("{\"value\":\"ok\"}", 201);

    assertThat(entry.getStoreId()).isNotNull();
    assertThat(entry.getScope()).isEqualTo("public-order-create");
    assertThat(entry.getKeyValue()).isEqualTo("idem-123");
    assertThat(entry.getRequestHash()).isEqualTo("request-hash");
    assertThat(entry.getResponseBody()).isEqualTo("{\"value\":\"ok\"}");
    assertThat(entry.getHttpStatus()).isEqualTo(201);
    assertThat(entry.getExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void shouldRejectNullResponseBodyWhenCompletingEntry() {
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "public-order-create",
            "idem-123",
            "request-hash",
            OffsetDateTime.now().plusDays(1));

    assertThatThrownBy(() -> entry.complete(null, 201))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("responseBody is required");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    var constructor = IdempotencyKeyEntry.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThat(constructor.newInstance()).isNotNull();
  }
}
