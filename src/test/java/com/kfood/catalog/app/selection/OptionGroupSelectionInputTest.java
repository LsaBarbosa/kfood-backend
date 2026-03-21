package com.kfood.catalog.app.selection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OptionGroupSelectionInputTest {

  @Test
  void shouldExposeSelectionInputValues() {
    var groupId = UUID.randomUUID();
    var itemId = UUID.randomUUID();

    var input = new OptionGroupSelectionInput(groupId, List.of(itemId));

    assertThat(input.optionGroupId()).isEqualTo(groupId);
    assertThat(input.optionItemIds()).containsExactly(itemId);
  }
}
