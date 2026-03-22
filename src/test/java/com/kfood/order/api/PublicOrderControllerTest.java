package com.kfood.order.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PublicOrderControllerTest {

  @Test
  void shouldRejectWhenCreatePublicOrderServiceIsUnavailable() {
    var controller =
        new PublicOrderController(
            new ObjectProvider<>() {
              @Override
              public com.kfood.order.app.CreatePublicOrderService getObject(Object... args) {
                return null;
              }

              @Override
              public com.kfood.order.app.CreatePublicOrderService getIfAvailable() {
                return null;
              }

              @Override
              public com.kfood.order.app.CreatePublicOrderService getIfUnique() {
                return null;
              }
            });

    assertThatThrownBy(() -> controller.create("loja-do-bairro", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("CreatePublicOrderService is not available.");
  }
}
