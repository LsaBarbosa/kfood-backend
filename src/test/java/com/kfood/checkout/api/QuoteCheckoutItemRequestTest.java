package com.kfood.checkout.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteCheckoutItemRequestTest {

  @Test
  void shouldConvertToCommandWithEmptyOptionsWhenOptionsAreNull() {
    var request = new QuoteCheckoutItemRequest(UUID.randomUUID(), 2, "notes", null);

    var command = request.toCommand();

    assertThat(command.productId()).isEqualTo(request.productId());
    assertThat(command.quantity()).isEqualTo(2);
    assertThat(command.notes()).isEqualTo("notes");
    assertThat(command.options()).isEmpty();
  }

  @Test
  void shouldConvertToCommandWithMappedOptionsWhenPresent() {
    var optionItemId = UUID.randomUUID();
    var request =
        new QuoteCheckoutItemRequest(
            UUID.randomUUID(),
            1,
            null,
            List.of(new QuoteCheckoutItemOptionRequest(optionItemId, 3)));

    var command = request.toCommand();

    assertThat(command.options()).hasSize(1);
    assertThat(command.options().getFirst().optionItemId()).isEqualTo(optionItemId);
    assertThat(command.options().getFirst().quantity()).isEqualTo(3);
  }
}
