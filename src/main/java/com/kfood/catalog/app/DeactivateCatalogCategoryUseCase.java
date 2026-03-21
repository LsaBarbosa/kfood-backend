package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogCategoryResponse;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
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
  CatalogCategoryRepository.class,
  CurrentTenantProvider.class,
  StoreOperationalGuard.class
})
public class DeactivateCatalogCategoryUseCase {

  private final StoreRepository storeRepository;
  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CurrentTenantProvider currentTenantProvider;
  private final StoreOperationalGuard storeOperationalGuard;

  public DeactivateCatalogCategoryUseCase(
      StoreRepository storeRepository,
      CatalogCategoryRepository catalogCategoryRepository,
      CurrentTenantProvider currentTenantProvider,
      StoreOperationalGuard storeOperationalGuard) {
    this.storeRepository = storeRepository;
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.currentTenantProvider = currentTenantProvider;
    this.storeOperationalGuard = storeOperationalGuard;
  }

  @Transactional
  public CatalogCategoryResponse execute(UUID categoryId) {
    var storeId = currentTenantProvider.getRequiredStoreId();
    var store =
        storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException(storeId));

    storeOperationalGuard.ensureStoreIsNotSuspended(store);

    var category =
        catalogCategoryRepository
            .findByIdAndStoreId(categoryId, storeId)
            .orElseThrow(() -> new CatalogCategoryNotFoundException(categoryId));

    category.deactivate();

    return CatalogCategoryMapper.toResponse(catalogCategoryRepository.saveAndFlush(category));
  }
}
