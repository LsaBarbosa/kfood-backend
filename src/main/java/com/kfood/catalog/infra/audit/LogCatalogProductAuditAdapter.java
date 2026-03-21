package com.kfood.catalog.infra.audit;

import com.kfood.catalog.app.audit.CatalogProductAuditPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogCatalogProductAuditAdapter implements CatalogProductAuditPort {

  private static final Logger log = LoggerFactory.getLogger(LogCatalogProductAuditAdapter.class);

  @Override
  public void recordProductPauseChanged(
      UUID storeId, UUID productId, boolean paused, String reason, UUID actorUserId) {
    log.info(
        "catalog.product.pause.changed storeId={} productId={} paused={} reason={} actorUserId={}",
        storeId,
        productId,
        paused,
        reason,
        actorUserId);
  }
}
