package com.kfood.merchant.app.audit;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import java.time.Instant;
import java.util.UUID;

public interface MerchantStoreAuditPort {

  void recordTermsAccepted(
      UUID storeId,
      UUID actorUserId,
      UUID acceptanceId,
      LegalDocumentType documentType,
      String documentVersion,
      Instant acceptedAt);

  void recordStoreStatusChanged(
      UUID storeId, UUID actorUserId, StoreStatus beforeStatus, StoreStatus afterStatus);
}
