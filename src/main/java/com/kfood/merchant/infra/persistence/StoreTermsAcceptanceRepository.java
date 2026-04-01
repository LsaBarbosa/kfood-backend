package com.kfood.merchant.infra.persistence;

import com.kfood.merchant.domain.LegalDocumentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreTermsAcceptanceRepository extends JpaRepository<StoreTermsAcceptance, UUID> {

  boolean existsByStoreId(UUID storeId);

  boolean existsByStoreIdAndDocumentType(UUID storeId, LegalDocumentType documentType);

  List<StoreTermsAcceptance> findAllByStoreIdOrderByAcceptedAtDesc(UUID storeId);
}
