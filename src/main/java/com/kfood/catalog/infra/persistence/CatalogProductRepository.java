package com.kfood.catalog.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

  boolean existsByStoreId(UUID storeId);

  Optional<CatalogProduct> findByIdAndStoreId(UUID id, UUID storeId);

  @EntityGraph(attributePaths = "category")
  List<CatalogProduct> findAllByStoreIdAndActiveTrueAndPausedFalseOrderBySortOrderAscNameAsc(
      UUID storeId);

  @EntityGraph(attributePaths = "category")
  List<CatalogProduct> findAllByStoreIdOrderBySortOrderAscNameAsc(UUID storeId);

  @Query(
      """
      select product
      from CatalogProduct product
      join fetch product.category category
      where product.store.id = :storeId
        and product.active = true
        and product.paused = false
        and category.store.id = :storeId
        and category.active = true
      order by category.sortOrder asc, category.name asc, product.sortOrder asc, product.name asc
      """)
  List<CatalogProduct> findAllVisibleForPublicMenu(UUID storeId);
}
