package com.kfood.shared.idempotency;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.shared.exceptions.BusinessException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {

  @Test
  void shouldRejectSameKeyWithDifferentPayload() {
    var repository = mock(IdempotencyKeyRepository.class);
    var service = new IdempotencyService(repository, new ObjectMapper());
    var storeId = UUID.randomUUID();
    var existing =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            "public-order-create",
            "idem-123",
            "abc123",
            OffsetDateTime.now().plusDays(1));
    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, "public-order-create", "idem-123"))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                service.execute(
                    storeId,
                    "public-order-create",
                    "idem-123",
                    new DummyRequest("payload-diferente"),
                    DummyResponse.class,
                    () -> new DummyResponse("ok")))
        .isInstanceOf(BusinessException.class);
  }

  private record DummyRequest(String value) {}

  private record DummyResponse(String value) {}
}
