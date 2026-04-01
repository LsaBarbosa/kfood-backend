package com.kfood.merchant.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantStoreAuditEventRepository
    extends JpaRepository<MerchantStoreAuditEvent, UUID> {

  List<MerchantStoreAuditEvent> findAllByStoreIdOrderByOccurredAtAsc(UUID storeId);
}
