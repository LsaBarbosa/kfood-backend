package com.kfood.shared.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
    this(idempotencyKeyRepository, new ObjectMapper().findAndRegisterModules());
  }

  IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
    this.idempotencyKeyRepository = idempotencyKeyRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public <T> T execute(
      UUID storeId,
      String scope,
      String key,
      Object request,
      Class<T> responseType,
      Supplier<T> action) {
    var requestHash = sha256(request);
    var existing = idempotencyKeyRepository.findByStoreIdAndScopeAndKeyValue(storeId, scope, key);
    if (existing.isPresent()) {
      return resolveExisting(existing.get(), requestHash, responseType);
    }

    try {
      idempotencyKeyRepository.save(
          new IdempotencyKeyEntry(
              UUID.randomUUID(),
              storeId,
              scope,
              key,
              requestHash,
              OffsetDateTime.now().plusDays(1)));
    } catch (DataIntegrityViolationException exception) {
      var racedEntry =
          idempotencyKeyRepository
              .findByStoreIdAndScopeAndKeyValue(storeId, scope, key)
              .orElseThrow();
      return resolveExisting(racedEntry, requestHash, responseType);
    }

    var response = action.get();
    var createdEntry =
        idempotencyKeyRepository
            .findByStoreIdAndScopeAndKeyValue(storeId, scope, key)
            .orElseThrow();
    createdEntry.complete(write(response), HttpStatus.CREATED.value());
    return response;
  }

  private <T> T resolveExisting(
      IdempotencyKeyEntry entry, String requestHash, Class<T> responseType) {
    if (!entry.getRequestHash().equals(requestHash)) {
      throw new BusinessException(
          ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD,
          "The same idempotency key was reused with a different payload.",
          HttpStatus.CONFLICT);
    }
    if (entry.getResponseBody() != null) {
      return read(entry.getResponseBody(), responseType);
    }
    throw new BusinessException(
        ErrorCode.VALIDATION_ERROR,
        "Idempotent request is still being processed.",
        HttpStatus.CONFLICT);
  }

  private String sha256(Object request) {
    try {
      var json = objectMapper.writeValueAsString(request);
      var digest =
          MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to hash request for idempotency", exception);
    }
  }

  private String write(Object response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize idempotent response", exception);
    }
  }

  private <T> T read(String body, Class<T> responseType) {
    try {
      return objectMapper.readValue(body, responseType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize idempotent response", exception);
    }
  }
}
