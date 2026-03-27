package com.kfood.catalog.app.selection;

import java.util.List;
import java.util.UUID;

public interface CatalogOptionGroupView {

  UUID getId();

  String getName();

  int getMinSelect();

  int getMaxSelect();

  List<? extends CatalogOptionItemView> getItems();
}
