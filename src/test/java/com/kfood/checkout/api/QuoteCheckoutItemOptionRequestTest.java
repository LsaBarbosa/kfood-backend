package com.kfood.checkout.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteCheckoutItemOptionRequestTest {

  @Test
  void shouldConvertRequestToCommand() {
    var optionItemId = UUID.randomUUID();
    var request = new QuoteCheckoutItemOptionRequest(optionItemId, 2);

    var command = request.toCommand();

    assertThat(command.optionItemId()).isEqualTo(optionItemId);
    assertThat(command.quantity()).isEqualTo(2);
  }
}
