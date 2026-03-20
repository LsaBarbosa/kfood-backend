package com.kfood.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.kfood.identity.app.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RbacTestControllerUnitTest {

  private final RbacTestController controller = new RbacTestController();

  @Test
  void shouldReturnCreateStoreResource() {
    var response = controller.createStore(user());

    assertThat(response).containsEntry("resource", "merchant.store.create");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnStoreResource() {
    var response = controller.getStore(user());

    assertThat(response).containsEntry("resource", "merchant.store.read");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnAcceptTermsResource() {
    var response = controller.acceptTerms(user());

    assertThat(response).containsEntry("resource", "merchant.terms.accept");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnCreateMerchantUserResource() {
    var response = controller.createMerchantUser(user());

    assertThat(response).containsEntry("resource", "merchant.user.create");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnCreateCategoryResource() {
    var response = controller.createCategory(user());

    assertThat(response).containsEntry("resource", "catalog.category.create");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnOrdersResource() {
    var response = controller.getOrders(user());

    assertThat(response).containsEntry("resource", "orders.read");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnOrderStatusUpdateResource() {
    var response = controller.updateOrderStatus("order-1", user());

    assertThat(response).containsEntry("resource", "orders.status.update");
    assertThat(response).containsEntry("orderId", "order-1");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  @Test
  void shouldReturnAuditLogsResource() {
    var response = controller.getAuditLogs(user());

    assertThat(response).containsEntry("resource", "admin.audit-logs.read");
    assertThat(response).containsEntry("by", "owner@kfood.local");
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(
        UUID.randomUUID(), "owner@kfood.local", UUID.randomUUID(), List.of("OWNER"));
  }
}
