package com.kfood.shared.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class IdempotencyServiceAdditionalTest {

  @Test
  void shouldReturnStoredResponseWhenExistingEntryIsCompleted() {
    var repository = mock(IdempotencyKeyRepository.class);
    var service = new IdempotencyService(repository, new ObjectMapper());
    var storeId = UUID.randomUUID();
    var response = new DummyResponse("ok");
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            "public-order-create",
            "idem-123",
            hashOf(new ObjectMapper(), new DummyRequest("payload")),
            OffsetDateTime.now().plusDays(1));
    entry.complete("{\"value\":\"ok\"}", 201);
    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, "public-order-create", "idem-123"))
        .thenReturn(Optional.of(entry));

    var result =
        service.execute(
            storeId,
            "public-order-create",
            "idem-123",
            new DummyRequest("payload"),
            DummyResponse.class,
            () -> new DummyResponse("new"));

    assertThat(result.value()).isEqualTo("ok");
  }

  @Test
  void shouldRejectWhenExistingRequestIsStillProcessing() {
    var repository = mock(IdempotencyKeyRepository.class);
    var service = new IdempotencyService(repository, new ObjectMapper());
    var storeId = UUID.randomUUID();
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            "public-order-create",
            "idem-123",
            hashOf(new ObjectMapper(), new DummyRequest("payload")),
            OffsetDateTime.now().plusDays(1));
    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, "public-order-create", "idem-123"))
        .thenReturn(Optional.of(entry));

    assertThatThrownBy(
            () ->
                service.execute(
                    storeId,
                    "public-order-create",
                    "idem-123",
                    new DummyRequest("payload"),
                    DummyResponse.class,
                    () -> new DummyResponse("new")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Idempotent request is still being processed.")
        .extracting("errorCode")
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void shouldPersistNewRequestAndCompleteEntry() {
    var repository = mock(IdempotencyKeyRepository.class);
    var objectMapper = new ObjectMapper();
    var service = new IdempotencyService(repository, objectMapper);
    var storeId = UUID.randomUUID();
    var scope = "public-order-create";
    var key = "idem-123";
    var created =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            scope,
            key,
            hashOf(objectMapper, new DummyRequest("payload")),
            OffsetDateTime.now().plusDays(1));

    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, scope, key))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(created));

    var result =
        service.execute(
            storeId,
            scope,
            key,
            new DummyRequest("payload"),
            DummyResponse.class,
            () -> new DummyResponse("ok"));

    assertThat(result.value()).isEqualTo("ok");
    assertThat(created.getResponseBody()).isEqualTo("{\"value\":\"ok\"}");
    assertThat(created.getHttpStatus()).isEqualTo(201);
    verify(repository).save(any(IdempotencyKeyEntry.class));
  }

  @Test
  void shouldResolveRaceConditionUsingPersistedEntry() {
    var repository = mock(IdempotencyKeyRepository.class);
    var objectMapper = new ObjectMapper();
    var service = new IdempotencyService(repository, objectMapper);
    var storeId = UUID.randomUUID();
    var scope = "public-order-create";
    var key = "idem-123";
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            scope,
            key,
            hashOf(objectMapper, new DummyRequest("payload")),
            OffsetDateTime.now().plusDays(1));
    entry.complete("{\"value\":\"ok\"}", 201);

    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, scope, key))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(entry));
    doThrow(new DataIntegrityViolationException("duplicate"))
        .when(repository)
        .save(any(IdempotencyKeyEntry.class));

    var result =
        service.execute(
            storeId,
            scope,
            key,
            new DummyRequest("payload"),
            DummyResponse.class,
            () -> new DummyResponse("new"));

    assertThat(result.value()).isEqualTo("ok");
  }

  @Test
  void shouldFailWhenHashSerializationBreaks() {
    var repository = mock(IdempotencyKeyRepository.class);
    var service =
        new IdempotencyService(
            repository,
            new ObjectMapper() {
              @Override
              public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
              }
            });

    assertThatThrownBy(
            () ->
                service.execute(
                    UUID.randomUUID(),
                    "scope",
                    "key",
                    new DummyRequest("payload"),
                    DummyResponse.class,
                    () -> new DummyResponse("ok")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to hash request for idempotency");
  }

  @Test
  void shouldFailWhenResponseSerializationBreaks() {
    var repository = mock(IdempotencyKeyRepository.class);
    var service =
        new IdempotencyService(
            repository,
            new ObjectMapper() {
              @Override
              public String writeValueAsString(Object value) throws JsonProcessingException {
                if (value instanceof DummyResponse) {
                  throw new JsonProcessingException("boom") {};
                }
                return super.writeValueAsString(value);
              }
            });
    var storeId = UUID.randomUUID();
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            "scope",
            "key",
            hashOf(new ObjectMapper(), new DummyRequest("payload")),
            OffsetDateTime.now().plusDays(1));
    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, "scope", "key"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(entry));

    assertThatThrownBy(
            () ->
                service.execute(
                    storeId,
                    "scope",
                    "key",
                    new DummyRequest("payload"),
                    DummyResponse.class,
                    () -> new DummyResponse("ok")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to serialize idempotent response");
  }

  @Test
  void shouldFailWhenStoredResponseDeserializationBreaks() {
    var repository = mock(IdempotencyKeyRepository.class);
    var service =
        new IdempotencyService(
            repository,
            new ObjectMapper() {
              @Override
              public <T> T readValue(String content, Class<T> valueType)
                  throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
              }
            });
    var storeId = UUID.randomUUID();
    var entry =
        new IdempotencyKeyEntry(
            UUID.randomUUID(),
            storeId,
            "scope",
            "key",
            hashOf(new ObjectMapper(), new DummyRequest("payload")),
            OffsetDateTime.now().plusDays(1));
    entry.complete("{\"value\":\"ok\"}", 201);
    when(repository.findByStoreIdAndScopeAndKeyValue(storeId, "scope", "key"))
        .thenReturn(Optional.of(entry));

    assertThatThrownBy(
            () ->
                service.execute(
                    storeId,
                    "scope",
                    "key",
                    new DummyRequest("payload"),
                    DummyResponse.class,
                    () -> new DummyResponse("new")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to deserialize idempotent response");
  }

  private static String hashOf(ObjectMapper objectMapper, Object value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(objectMapper.writeValueAsBytes(value)));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private record DummyRequest(String value) {}

  private record DummyResponse(String value) {}
}
