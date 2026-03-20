package com.kfood.identity.api;

import com.kfood.identity.app.AuthenticatedUser;
import com.kfood.identity.app.Roles;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rbac")
public class RbacTestController {

  @PostMapping("/merchant/store")
  @PreAuthorize(Roles.OWNER_OR_ADMIN)
  public Map<String, Object> createStore(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "merchant.store.create", "by", user.getUsername());
  }

  @GetMapping("/merchant/store")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public Map<String, Object> getStore(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "merchant.store.read", "by", user.getUsername());
  }

  @PostMapping("/merchant/store/terms-acceptance")
  @PreAuthorize(Roles.OWNER)
  public Map<String, Object> acceptTerms(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "merchant.terms.accept", "by", user.getUsername());
  }

  @PostMapping("/merchant/users")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public Map<String, Object> createMerchantUser(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "merchant.user.create", "by", user.getUsername());
  }

  @PostMapping("/catalog/categories")
  @PreAuthorize(Roles.OWNER_OR_MANAGER)
  public Map<String, Object> createCategory(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "catalog.category.create", "by", user.getUsername());
  }

  @GetMapping("/orders")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public Map<String, Object> getOrders(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "orders.read", "by", user.getUsername());
  }

  @PatchMapping("/orders/{orderId}/status")
  @PreAuthorize(Roles.OWNER_MANAGER_ATTENDANT)
  public Map<String, Object> updateOrderStatus(
      @PathVariable String orderId, @AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "orders.status.update", "orderId", orderId, "by", user.getUsername());
  }

  @GetMapping("/admin/audit-logs")
  @PreAuthorize(Roles.ADMIN)
  public Map<String, Object> getAuditLogs(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of("resource", "admin.audit-logs.read", "by", user.getUsername());
  }
}
