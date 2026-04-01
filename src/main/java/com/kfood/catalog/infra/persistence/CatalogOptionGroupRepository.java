package com.kfood.catalog.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogOptionGroupRepository extends JpaRepository<CatalogOptionGroup, UUID> {

  @EntityGraph(attributePaths = "items")
  List<CatalogOptionGroup> findAllByProduct_IdAndActiveTrueOrderByIdAsc(UUID productId);

  @EntityGraph(attributePaths = "items")
  List<CatalogOptionGroup> findAllByProduct_IdInAndActiveTrueOrderByProduct_IdAscIdAsc(
      List<UUID> productIds);
}
