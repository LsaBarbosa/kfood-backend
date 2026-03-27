package com.kfood.catalog.infra.persistence;

import com.kfood.catalog.app.selection.CatalogOptionGroupLookup;
import com.kfood.catalog.app.selection.CatalogOptionGroupView;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(CatalogOptionGroupRepository.class)
public class CatalogOptionGroupLookupAdapter implements CatalogOptionGroupLookup {

  private final CatalogOptionGroupRepository catalogOptionGroupRepository;

  public CatalogOptionGroupLookupAdapter(
      CatalogOptionGroupRepository catalogOptionGroupRepository) {
    this.catalogOptionGroupRepository = catalogOptionGroupRepository;
  }

  @Override
  public List<CatalogOptionGroupView> findActiveByProductId(UUID productId) {
    return List.copyOf(
        catalogOptionGroupRepository.findAllByProduct_IdAndActiveTrueOrderByIdAsc(productId));
  }
}
