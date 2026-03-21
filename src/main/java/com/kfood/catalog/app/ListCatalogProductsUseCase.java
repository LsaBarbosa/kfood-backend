package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogProductResponse;
import com.kfood.catalog.infra.persistence.CatalogProductRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({CatalogProductRepository.class, CurrentTenantProvider.class})
public class ListCatalogProductsUseCase {

  private final CatalogProductRepository catalogProductRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public ListCatalogProductsUseCase(
      CatalogProductRepository catalogProductRepository,
      CurrentTenantProvider currentTenantProvider) {
    this.catalogProductRepository = catalogProductRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public List<CatalogProductResponse> execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();

    return catalogProductRepository.findAllByStoreIdOrderBySortOrderAscNameAsc(storeId).stream()
        .map(CatalogProductMapper::toResponse)
        .toList();
  }
}
