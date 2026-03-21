package com.kfood.catalog.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogOptionItemRepository extends JpaRepository<CatalogOptionItem, UUID> {

  List<CatalogOptionItem> findAllByOptionGroupIdOrderBySortOrderAscIdAsc(UUID optionGroupId);
}
