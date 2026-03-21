package com.kfood.catalog.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogOptionGroupRepository extends JpaRepository<CatalogOptionGroup, UUID> {}
