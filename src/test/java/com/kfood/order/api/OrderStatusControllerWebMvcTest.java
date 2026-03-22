package com.kfood.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.order.app.UpdateOrderStatusUseCase;
import com.kfood.order.domain.OrderStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
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
class OrderStatusControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private UpdateOrderStatusUseCase updateOrderStatusUseCase;

  @Test
  void shouldUpdateOrderStatusSuccessfully() throws Exception {
    var orderId = UUID.randomUUID();
    var actorUserId = UUID.randomUUID();

    when(updateOrderStatusUseCase.execute(eq(orderId), any(UpdateOrderStatusRequest.class)))
        .thenReturn(
            new UpdateOrderStatusResponse(
                orderId,
                OrderStatus.NEW,
                OrderStatus.PREPARING,
                Instant.parse("2026-03-22T18:40:00Z"),
                actorUserId));

    mockMvc
        .perform(
            patch("/v1/orders/{orderId}/status", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "newStatus": "PREPARING",
                      "reason": "Order entered preparation"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId.toString()))
        .andExpect(jsonPath("$.previousStatus").value("NEW"))
        .andExpect(jsonPath("$.newStatus").value("PREPARING"))
        .andExpect(jsonPath("$.changedAt").value("2026-03-22T18:40:00Z"))
        .andExpect(jsonPath("$.changedBy").value(actorUserId.toString()));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    var orderId = UUID.randomUUID();

    mockMvc
        .perform(
            patch("/v1/orders/{orderId}/status", orderId)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "newStatus": "PREPARING"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldRejectInvalidRequestBody() throws Exception {
    var orderId = UUID.randomUUID();

    mockMvc
        .perform(
            patch("/v1/orders/{orderId}/status", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content("{\"reason\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("newStatus"));
  }

  @Test
  void shouldReturnConflictWhenTransitionIsInvalid() throws Exception {
    var orderId = UUID.randomUUID();

    when(updateOrderStatusUseCase.execute(eq(orderId), any(UpdateOrderStatusRequest.class)))
        .thenThrow(
            new BusinessException(
                ErrorCode.ORDER_STATUS_TRANSITION_INVALID,
                "Invalid order status transition from NEW to READY",
                HttpStatus.CONFLICT));

    mockMvc
        .perform(
            patch("/v1/orders/{orderId}/status", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "newStatus": "READY"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORDER_STATUS_TRANSITION_INVALID"))
        .andExpect(
            jsonPath("$.message").value("Invalid order status transition from NEW to READY"));
  }

  @Test
  void shouldForbidAdminBecauseRouteIsTenantScoped() throws Exception {
    mockMvc
        .perform(
            patch("/v1/orders/{orderId}/status", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ADMIN))
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"PREPARING\"}"))
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
