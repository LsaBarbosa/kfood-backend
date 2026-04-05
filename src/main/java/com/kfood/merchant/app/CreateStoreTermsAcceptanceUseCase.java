package com.kfood.merchant.app;

import com.kfood.merchant.app.audit.MerchantStoreAuditPort;
import com.kfood.merchant.app.port.MerchantCommandPort;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  MerchantCommandPort.class,
  MerchantStoreAuditPort.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class
})
public class CreateStoreTermsAcceptanceUseCase {

  private final MerchantCommandPort merchantCommandPort;
  private final MerchantStoreAuditPort merchantStoreAuditPort;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final Clock clock;

  public CreateStoreTermsAcceptanceUseCase(
      MerchantCommandPort merchantCommandPort,
      MerchantStoreAuditPort merchantStoreAuditPort,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
      Clock clock) {
    this.merchantCommandPort = merchantCommandPort;
    this.merchantStoreAuditPort = merchantStoreAuditPort;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    this.clock = clock;
  }

  @Transactional
  public StoreTermsAcceptanceOutput execute(
      CreateStoreTermsAcceptanceCommand command, String requestIp) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var authenticatedUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var acceptedAt = Instant.now(clock);
    var result =
        merchantCommandPort.createStoreTermsAcceptance(
            storeId, authenticatedUserId, command, normalizeRequestIp(requestIp), acceptedAt);
    merchantStoreAuditPort.recordTermsAccepted(
        storeId,
        authenticatedUserId,
        result.id(),
        result.documentType(),
        result.documentVersion(),
        result.acceptedAt());
    return result;
  }

  private String normalizeRequestIp(String requestIp) {
    return Objects.requireNonNull(requestIp, "requestIp is required").trim();
  }
}
