package com.kfood.checkout.infra.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutQuoteRepository extends JpaRepository<CheckoutQuote, UUID> {

  @EntityGraph(attributePaths = {"items", "items.options"})
  Optional<CheckoutQuote> findByIdAndStoreIdAndExpiresAtAfter(
      UUID id, UUID storeId, OffsetDateTime now);
}
