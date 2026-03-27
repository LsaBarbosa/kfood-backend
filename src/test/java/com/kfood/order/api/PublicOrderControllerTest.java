package com.kfood.order.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.order.app.GetPublicOrderByNumberUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PublicOrderControllerTest {

  @Test
  void shouldRejectWhenCreatePublicOrderServiceIsUnavailable() {
    var controller = new PublicOrderController(unavailableProvider(), unavailableGetProvider());

    assertThatThrownBy(() -> controller.create("loja-do-bairro", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("CreatePublicOrderService is not available.");
  }

  @Test
  void shouldRejectWhenGetPublicOrderByNumberUseCaseIsUnavailable() {
    var controller = new PublicOrderController(unavailableProvider(), unavailableGetProvider());

    assertThatThrownBy(() -> controller.getByOrderNumber("loja-do-bairro", "PED-20260326-000123"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("GetPublicOrderByNumberUseCase is not available.");
  }

  private ObjectProvider<com.kfood.order.app.CreatePublicOrderService> unavailableProvider() {
    return new ObjectProvider<>() {
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
    };
  }

  private ObjectProvider<GetPublicOrderByNumberUseCase> unavailableGetProvider() {
    return new ObjectProvider<>() {
      @Override
      public GetPublicOrderByNumberUseCase getObject(Object... args) {
        return null;
      }

      @Override
      public GetPublicOrderByNumberUseCase getIfAvailable() {
        return null;
      }

      @Override
      public GetPublicOrderByNumberUseCase getIfUnique() {
        return null;
      }
    };
  }
}
