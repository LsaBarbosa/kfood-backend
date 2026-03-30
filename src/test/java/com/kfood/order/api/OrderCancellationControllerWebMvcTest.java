package com.kfood.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.order.app.CancelOrderCommand;
import com.kfood.order.app.CancelOrderOutput;
import com.kfood.order.app.CancelOrderUseCase;
import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "app.security.jwt-secret=12345678901234567890123456789012",
      "app.security.jwt-expiration-seconds=3600"
    })
class OrderCancellationControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CancelOrderUseCase cancelOrderUseCase;

  @Test
  void shouldCancelOrderSuccessfully() throws Exception {
    var orderId = UUID.randomUUID();

    when(cancelOrderUseCase.execute(eq(orderId), any(CancelOrderCommand.class)))
        .thenReturn(
            new CancelOrderOutput(
                orderId,
                OrderStatus.CANCELED,
                Instant.parse("2026-03-22T19:10:00Z"),
                "Customer gave up on the order"));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/cancel", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": "Customer gave up on the order"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId.toString()))
        .andExpect(jsonPath("$.status").value("CANCELED"))
        .andExpect(jsonPath("$.canceledAt").value("2026-03-22T19:10:00Z"))
        .andExpect(jsonPath("$.reason").value("Customer gave up on the order"));
  }

  @Test
  void shouldRejectBlankReason() throws Exception {
    var orderId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/cancel", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "reason": "   "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("reason"));
  }

  @Test
  void shouldRequireAuthenticationToCancelOrder() throws Exception {
    mockMvc
        .perform(
            post("/v1/orders/{orderId}/cancel", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content("{\"reason\":\"Customer gave up on the order\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldForbidAttendantToCancelOrder() throws Exception {
    mockMvc
        .perform(
            post("/v1/orders/{orderId}/cancel", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content("{\"reason\":\"Customer gave up on the order\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  private String tokenOf(UserRoleName role) {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            role.name().toLowerCase() + "@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(role));
    return jwtTokenService.generateToken(user);
  }
}
