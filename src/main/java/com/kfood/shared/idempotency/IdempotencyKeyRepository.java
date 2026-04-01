package com.kfood.shared.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntry, UUID> {

  Optional<IdempotencyKeyEntry> findByStoreIdAndScopeAndKeyValue(
      UUID storeId, String scope, String keyValue);
}
