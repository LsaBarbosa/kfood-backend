package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductPauseResponse;
import com.kfood.catalog.api.UpdateCatalogProductPauseRequest;
import com.kfood.catalog.app.audit.CatalogProductAuditPort;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.StoreRepository;
import com.kfood.shared.security.CurrentAuthenticatedUserProvider;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({
  StoreRepository.class,
  CatalogProductRepository.class,
  CurrentTenantProvider.class,
  CurrentAuthenticatedUserProvider.class,
  StoreOperationalGuard.class,
  CatalogProductAuditPort.class
})
public class UpdateCatalogProductPauseUseCase {

  private final StoreRepository storeRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;
  private final StoreOperationalGuard storeOperationalGuard;
  private final CatalogProductAuditPort catalogProductAuditPort;

  public UpdateCatalogProductPauseUseCase(
      StoreRepository storeRepository,
      CatalogProductRepository catalogProductRepository,
      CurrentTenantProvider currentTenantProvider,
      CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider,
      StoreOperationalGuard storeOperationalGuard,
      CatalogProductAuditPort catalogProductAuditPort) {
    this.storeRepository = storeRepository;
    this.catalogProductRepository = catalogProductRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    this.storeOperationalGuard = storeOperationalGuard;
    this.catalogProductAuditPort = catalogProductAuditPort;
  }

  @Transactional
  public CatalogProductPauseResponse execute(
      UUID productId, UpdateCatalogProductPauseRequest request) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var actorUserId = currentAuthenticatedUserProvider.getRequiredUserId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var product =
        catalogProductRepository
            .findByIdAndStoreId(productId, storeId)
            .orElseThrow(() -> new CatalogProductNotFoundException(productId));

    if (request.paused()) {
      product.pause();
    } else {
      product.resume();
    }

    var savedProduct = catalogProductRepository.saveAndFlush(product);

    catalogProductAuditPort.recordProductPauseChanged(
        storeId, savedProduct.getId(), savedProduct.isPaused(), request.reason(), actorUserId);

    return new CatalogProductPauseResponse(
        savedProduct.getId(), savedProduct.isPaused(), savedProduct.isActive());
  }
}
