package com.kfood.catalog.app.selection;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductOptionSelectionValidator {

  private final ObjectProvider<CatalogOptionGroupLookup> catalogOptionGroupLookupProvider;

  public ProductOptionSelectionValidator(
      ObjectProvider<CatalogOptionGroupLookup> catalogOptionGroupLookupProvider) {
    this.catalogOptionGroupLookupProvider = catalogOptionGroupLookupProvider;
  }

  public void validate(UUID productId, List<OptionGroupSelectionInput> selections) {
    var catalogOptionGroupLookup = catalogOptionGroupLookupProvider.getIfAvailable();
    if (catalogOptionGroupLookup == null) {
      throw new IllegalStateException("CatalogOptionGroupLookup bean is required");
    }

    var activeGroups = catalogOptionGroupLookup.findActiveByProductId(productId);
    var groupsById =
        activeGroups.stream()
            .collect(Collectors.toMap(CatalogOptionGroupView::getId, Function.identity()));
    var selectionsByGroupId = indexSelections(groupsById, selections);

    for (var group : activeGroups) {
      var selectedItemIds = normalizeSelectedItemIds(selectionsByGroupId.get(group.getId()));
      validateSelectedItems(group, selectedItemIds);
      validateSelectionCount(group, selectedItemIds.size());
    }
  }

  private Map<UUID, OptionGroupSelectionInput> indexSelections(
      Map<UUID, ? extends CatalogOptionGroupView> groupsById,
      List<OptionGroupSelectionInput> selections) {
    var result = new HashMap<UUID, OptionGroupSelectionInput>();

    if (selections == null || selections.isEmpty()) {
      return result;
    }

    for (var selection : selections) {
      if (selection == null || selection.optionGroupId() == null) {
        throw validation("optionGroupId must be informed");
      }
      if (!groupsById.containsKey(selection.optionGroupId())) {
        throw validation("Option group does not belong to the product");
      }
      if (result.putIfAbsent(selection.optionGroupId(), selection) != null) {
        throw validation("Duplicate selection for option group id " + selection.optionGroupId());
      }
    }

    return result;
  }

  private Set<UUID> normalizeSelectedItemIds(OptionGroupSelectionInput selection) {
    if (selection == null
        || selection.optionItemIds() == null
        || selection.optionItemIds().isEmpty()) {
      return Set.of();
    }

    var ids = new LinkedHashSet<UUID>();
    for (var itemId : selection.optionItemIds()) {
      if (itemId == null) {
        throw validation("optionItemIds must not contain null values");
      }
      if (!ids.add(itemId)) {
        throw validation("Duplicate option item id " + itemId + " in same group");
      }
    }
    return ids;
  }

  private void validateSelectedItems(CatalogOptionGroupView group, Set<UUID> selectedItemIds) {
    var activeItemIds =
        group.getItems().stream()
            .filter(item -> item.isActive())
            .map(item -> item.getId())
            .collect(Collectors.toSet());

    for (var selectedItemId : selectedItemIds) {
      if (!activeItemIds.contains(selectedItemId)) {
        throw validation(
            "Option item id "
                + selectedItemId
                + " does not belong to group '"
                + group.getName()
                + "' or is inactive");
      }
    }
  }

  private void validateSelectionCount(CatalogOptionGroupView group, int selectedCount) {
    if (selectedCount < group.getMinSelect()) {
      throw validation(
          "Selection below minimum for group '"
              + group.getName()
              + "'. Minimum: "
              + group.getMinSelect()
              + ", selected: "
              + selectedCount);
    }
    if (selectedCount > group.getMaxSelect()) {
      throw validation(
          "Selection above maximum for group '"
              + group.getName()
              + "'. Maximum: "
              + group.getMaxSelect()
              + ", selected: "
              + selectedCount);
    }
  }

  private BusinessException validation(String message) {
    return new BusinessException(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST);
  }
}
