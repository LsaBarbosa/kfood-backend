package com.kfood.merchant.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreTermsAcceptanceRepository extends JpaRepository<StoreTermsAcceptance, UUID> {

  boolean existsByStoreId(UUID storeId);

  List<StoreTermsAcceptance> findAllByStoreIdOrderByAcceptedAtDesc(UUID storeId);
}
