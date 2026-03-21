package com.kfood.catalog.app.audit;

import java.util.UUID;

public interface CatalogProductAuditPort {

  void recordProductPauseChanged(
      UUID storeId, UUID productId, boolean paused, String reason, UUID actorUserId);
}
