package com.kfood.catalog.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {

  boolean existsByStoreId(UUID storeId);

  Optional<CatalogCategory> findByIdAndStoreId(UUID id, UUID storeId);

  List<CatalogCategory> findAllByStoreIdOrderBySortOrderAscNameAsc(UUID storeId);
}
