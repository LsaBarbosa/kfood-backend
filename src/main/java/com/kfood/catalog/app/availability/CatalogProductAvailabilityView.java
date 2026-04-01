package com.kfood.catalog.app.availability;

import java.util.List;

public interface CatalogProductAvailabilityView {

  boolean isActive();

  boolean isPaused();

  List<? extends CatalogAvailabilityWindowView> getAvailabilityWindows();
}
