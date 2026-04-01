package com.kfood.merchant.infra.audit;

import com.kfood.merchant.app.audit.MerchantStoreAuditPort;
import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
import com.kfood.merchant.infra.persistence.MerchantStoreAuditEvent;
import com.kfood.merchant.infra.persistence.MerchantStoreAuditEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaMerchantStoreAuditAdapter implements MerchantStoreAuditPort {

  private static final String TERMS_ACCEPTED_EVENT_TYPE = "LEGAL_TERMS_ACCEPTED";
  private static final String STATUS_CHANGED_EVENT_TYPE = "STORE_STATUS_CHANGED";
  private static final String STORE_ENTITY_TYPE = "STORE";
  private static final String TERMS_ACCEPTANCE_ENTITY_TYPE = "STORE_TERMS_ACCEPTANCE";

  private final MerchantStoreAuditEventRepository merchantStoreAuditEventRepository;
  private final Clock clock;

  public JpaMerchantStoreAuditAdapter(
      MerchantStoreAuditEventRepository merchantStoreAuditEventRepository, Clock clock) {
    this.merchantStoreAuditEventRepository = merchantStoreAuditEventRepository;
    this.clock = clock;
  }

  @Override
  public void recordTermsAccepted(
      UUID storeId,
      UUID actorUserId,
      UUID acceptanceId,
      LegalDocumentType documentType,
      String documentVersion,
      Instant acceptedAt) {
    merchantStoreAuditEventRepository.saveAndFlush(
        new MerchantStoreAuditEvent(
            UUID.randomUUID(),
            storeId,
            actorUserId,
            TERMS_ACCEPTED_EVENT_TYPE,
            TERMS_ACCEPTANCE_ENTITY_TYPE,
            acceptanceId,
            acceptedAt,
            null,
            null,
            documentType,
            documentVersion,
            acceptedAt));
  }

  @Override
  public void recordStoreStatusChanged(
      UUID storeId, UUID actorUserId, StoreStatus beforeStatus, StoreStatus afterStatus) {
    merchantStoreAuditEventRepository.saveAndFlush(
        new MerchantStoreAuditEvent(
            UUID.randomUUID(),
            storeId,
            actorUserId,
            STATUS_CHANGED_EVENT_TYPE,
            STORE_ENTITY_TYPE,
            storeId,
            Instant.now(clock),
            beforeStatus,
            afterStatus,
            null,
            null,
            null));
  }
}
