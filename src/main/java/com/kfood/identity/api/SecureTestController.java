package com.kfood.identity.api;

import com.kfood.identity.app.AuthenticatedUser;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecureTestController {

  @GetMapping("/v1/merchant/me")
  public Map<String, Object> me(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of(
        "userId",
        user.getUserId(),
        "email",
        user.getUsername(),
        "tenantId",
        user.getTenantId(),
        "roles",
        user.getAuthorities().stream().map(Object::toString).toList());
  }
}
