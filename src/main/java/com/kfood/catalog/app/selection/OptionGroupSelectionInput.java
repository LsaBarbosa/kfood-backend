package com.kfood.catalog.app.selection;

import java.util.List;
import java.util.UUID;

public record OptionGroupSelectionInput(UUID optionGroupId, List<UUID> optionItemIds) {}
