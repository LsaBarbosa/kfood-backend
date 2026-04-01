package com.kfood.catalog.app;

import com.kfood.catalog.api.CatalogCategoryResponse;
import com.kfood.catalog.infra.persistence.CatalogCategoryRepository;
import com.kfood.shared.tenancy.CurrentTenantProvider;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean({CatalogCategoryRepository.class, CurrentTenantProvider.class})
public class ListCatalogCategoriesUseCase {

  private final CatalogCategoryRepository catalogCategoryRepository;
  private final CurrentTenantProvider currentTenantProvider;

  public ListCatalogCategoriesUseCase(
      CatalogCategoryRepository catalogCategoryRepository,
      CurrentTenantProvider currentTenantProvider) {
    this.catalogCategoryRepository = catalogCategoryRepository;
    this.currentTenantProvider = currentTenantProvider;
  }

  @Transactional(readOnly = true)
  public List<CatalogCategoryResponse> execute() {
    var storeId = currentTenantProvider.getRequiredStoreId();

    return catalogCategoryRepository.findAllByStoreIdOrderBySortOrderAscNameAsc(storeId).stream()
        .map(CatalogCategoryMapper::toResponse)
        .toList();
  }
}
