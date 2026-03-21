package com.kfood.catalog.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

  boolean existsByStoreId(UUID storeId);

  Optional<CatalogProduct> findByIdAndStoreId(UUID id, UUID storeId);

  List<CatalogProduct> findAllByStoreIdAndActiveTrueAndPausedFalseOrderBySortOrderAscNameAsc(
      UUID storeId);

  List<CatalogProduct> findAllByStoreIdOrderBySortOrderAscNameAsc(UUID storeId);
}
