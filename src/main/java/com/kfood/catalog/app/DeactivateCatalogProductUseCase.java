package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductResponse;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.merchant.app.StoreNotFoundException;
import com.kfood.merchant.app.StoreOperationalGuard;
import com.kfood.merchant.infra.persistence.StoreRepository;
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
  StoreOperationalGuard.class
})
public class DeactivateCatalogProductUseCase {

  private final StoreRepository storeRepository;
  private final CatalogProductRepository catalogProductRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public DeactivateCatalogProductUseCase(
      StoreRepository storeRepository,
      CatalogProductRepository catalogProductRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.catalogProductRepository = catalogProductRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public CatalogProductResponse execute(UUID productId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var product =
        catalogProductRepository
            .findByIdAndStoreId(productId, storeId)
            .orElseThrow(() -> new CatalogProductNotFoundException(productId));

    product.deactivate();

    return CatalogProductMapper.toResponse(catalogProductRepository.saveAndFlush(product));
  }
}
