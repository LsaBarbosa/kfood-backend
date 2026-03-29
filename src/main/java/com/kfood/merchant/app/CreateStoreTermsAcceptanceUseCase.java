package com.kfood.merchant.app;

import com.kfood.identity.persistence.IdentityUserRepository;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptance;
import com.kfood.merchant.infra.persistence.StoreTermsAcceptanceRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  StoreTermsAcceptanceRepository.class,
  IdentityUserRepository.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class
})
public class CreateStoreTermsAcceptanceUseCase {

  private final StoreRepository storeRepository;
  private final StoreTermsAcceptanceRepository storeTermsAcceptanceRepository;
  private final IdentityUserRepository identityUserRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final Clock clock;

  public CreateStoreTermsAcceptanceUseCase(
      StoreRepository storeRepository,
      StoreTermsAcceptanceRepository storeTermsAcceptanceRepository,
      IdentityUserRepository identityUserRepository,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
      Clock clock) {
    this.storeRepository = storeRepository;
    this.storeTermsAcceptanceRepository = storeTermsAcceptanceRepository;
    this.identityUserRepository = identityUserRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    this.clock = clock;
  }

  @Transactional
  public StoreTermsAcceptanceOutput execute(
      CreateStoreTermsAcceptanceCommand command, String requestIp) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var authenticatedUserId = currentAuthenticatedUserProvider.getRequiredUserId();

    storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    var authenticatedUser =
        identityUserRepository
            .findById(authenticatedUserId)
            .orElseThrow(() -> new AuthenticatedUserNotFoundException(authenticatedUserId));

    if (!Objects.equals(authenticatedUser.getStoreId(), storeId)) {
      throw new TenantAccessDeniedException();
    }

    var acceptance =
        new StoreTermsAcceptance(
            UUID.randomUUID(),
            storeId,
            authenticatedUserId,
            command.documentType(),
            command.documentVersion(),
            Instant.now(clock),
            normalizeRequestIp(requestIp));

    var savedAcceptance = storeTermsAcceptanceRepository.saveAndFlush(acceptance);
    return new StoreTermsAcceptanceOutput(
        savedAcceptance.getId(),
        savedAcceptance.getDocumentType(),
        savedAcceptance.getDocumentVersion(),
        savedAcceptance.getAcceptedAt());
  }

  private String normalizeRequestIp(String requestIp) {
    return Objects.requireNonNull(requestIp, "requestIp is required").trim();
  }
}
