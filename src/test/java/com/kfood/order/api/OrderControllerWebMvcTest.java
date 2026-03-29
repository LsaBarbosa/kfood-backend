package com.kfood.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.order.app.ListOrdersOutput;
import com.kfood.order.app.ListOrdersUseCase;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class OrderControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private ListOrdersUseCase listOrdersUseCase;

  @Test
  void shouldListOperationalOrdersWithFilters() throws Exception {
    var orderId = UUID.randomUUID();
    when(listOrdersUseCase.execute(any(), any()))
        .thenReturn(
            new ListOrdersOutput(
                List.of(
                    new ListOrdersOutput.Item(
                        orderId,
                        "PED-20260322-000123",
                        OrderStatus.NEW,
                        PaymentStatusSnapshot.PENDING,
                        "Lucas Santana",
                        new BigDecimal("56.50"),
                        Instant.parse("2026-03-22T14:15:00Z"))),
                0,
                20,
                1,
                1,
                List.of("createdAt,desc")));

    mockMvc
        .perform(
            get("/v1/orders")
                .param("status", "NEW")
                .param("dateFrom", "2026-03-20")
                .param("dateTo", "2026-03-22")
                .param("fulfillmentType", "DELIVERY")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(orderId.toString()))
        .andExpect(jsonPath("$.items[0].orderNumber").value("PED-20260322-000123"))
        .andExpect(jsonPath("$.items[0].status").value("NEW"))
        .andExpect(jsonPath("$.items[0].paymentStatus").value("PENDING"))
        .andExpect(jsonPath("$.items[0].paymentStatusSnapshot").value("PENDING"))
        .andExpect(jsonPath("$.items[0].customerName").value("Lucas Santana"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
  }

  @Test
  void shouldRequireAuthenticationToListOrders() throws Exception {
    mockMvc
        .perform(get("/v1/orders"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldForbidAdminBecauseRouteIsTenantScoped() throws Exception {
    mockMvc
        .perform(
            get("/v1/orders")
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ADMIN, null)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  private String tokenOf(UserRoleName role) {
    return tokenOf(role, UUID.randomUUID());
  }

  private String tokenOf(UserRoleName role, UUID storeId) {
    var user =
        new IdentityUserEntity(
            UUID.randomUUID(),
            storeId,
            role.name().toLowerCase() + "@kfood.local",
            "$2a$10$hash",
            UserStatus.ACTIVE);
    user.replaceRoles(Set.of(role));
    return jwtTokenService.generateToken(user);
  }
}
