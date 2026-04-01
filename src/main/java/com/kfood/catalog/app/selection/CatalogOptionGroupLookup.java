package com.kfood.catalog.app.selection;

import java.util.List;
import java.util.UUID;

public interface CatalogOptionGroupLookup {

  List<CatalogOptionGroupView> findActiveByProductId(UUID productId);
}
