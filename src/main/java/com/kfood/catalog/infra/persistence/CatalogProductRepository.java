package com.kfood.catalog.infra.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

  boolean existsByStoreId(UUID storeId);

  Optional<CatalogProduct> findByIdAndStoreId(UUID id, UUID storeId);

  @EntityGraph(attributePaths = {"category", "availabilityWindows"})
  Optional<CatalogProduct> findDetailedByIdAndStoreId(UUID id, UUID storeId);

  @EntityGraph(attributePaths = {"availabilityWindows", "optionGroups", "optionGroups.items"})
  List<CatalogProduct> findAllByStoreIdAndIdIn(UUID storeId, Collection<UUID> ids);

  @EntityGraph(attributePaths = "category")
  List<CatalogProduct> findAllByStoreIdAndActiveTrueAndPausedFalseOrderBySortOrderAscNameAsc(
      UUID storeId);

  @EntityGraph(attributePaths = "category")
  List<CatalogProduct> findAllByStoreIdOrderBySortOrderAscNameAsc(UUID storeId);

  @Query(
      """
      select distinct product
      from CatalogProduct product
      join fetch product.category category
      left join fetch product.optionGroups optionGroup
      left join fetch optionGroup.items optionItem
      where product.store.id = :storeId
        and product.active = true
        and product.paused = false
        and category.store.id = :storeId
        and category.active = true
        and (
          not exists (
            select 1
            from CatalogProductAvailabilityWindow configuredWindow
            where configuredWindow.product = product
              and configuredWindow.active = true
          )
          or exists (
            select 1
            from CatalogProductAvailabilityWindow matchingWindow
            where matchingWindow.product = product
              and matchingWindow.active = true
              and matchingWindow.dayOfWeek = :dayOfWeek
              and :localTime >= matchingWindow.startTime
              and :localTime < matchingWindow.endTime
          )
        )
      order by category.sortOrder asc, category.name asc, product.sortOrder asc, product.name asc
      """)
  List<CatalogProduct> findAllVisibleForPublicMenu(
      UUID storeId, DayOfWeek dayOfWeek, LocalTime localTime);
}
