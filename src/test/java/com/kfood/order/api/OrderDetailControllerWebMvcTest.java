package com.kfood.order.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.order.app.GetOrderDetailUseCase;
import com.kfood.order.app.OrderDetailOutput;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentMethod;
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
class OrderDetailControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private GetOrderDetailUseCase getOrderDetailUseCase;

  @Test
  void shouldReturnOperationalOrderDetail() throws Exception {
    var orderId = UUID.randomUUID();

    when(getOrderDetailUseCase.execute(eq(orderId)))
        .thenReturn(
            new OrderDetailOutput(
                orderId,
                "PED-20260322-000123",
                OrderStatus.NEW,
                FulfillmentType.DELIVERY,
                new BigDecimal("50.00"),
                new BigDecimal("6.50"),
                new BigDecimal("56.50"),
                "Tocar campainha",
                null,
                Instant.parse("2026-03-22T18:25:00Z"),
                Instant.parse("2026-03-22T18:25:00Z"),
                new OrderDetailOutput.Customer(
                    UUID.randomUUID(), "Lucas Santana", "21999990000", "lucas@email.com"),
                new OrderDetailOutput.Address(
                    "Casa", "25000000", "Rua das Flores", "45", "Centro", "Mage", "RJ", "Ap 101"),
                new OrderDetailOutput.Payment(PaymentMethod.PIX, PaymentStatusSnapshot.PENDING),
                List.of(
                    new OrderDetailOutput.Item(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Pizza Calabresa",
                        new BigDecimal("42.00"),
                        1,
                        new BigDecimal("50.00"),
                        "Sem cebola",
                        List.of(
                            new OrderDetailOutput.ItemOption(
                                UUID.randomUUID(),
                                "Borda Catupiry",
                                new BigDecimal("8.00"),
                                1,
                                new BigDecimal("8.00")))))));

    mockMvc
        .perform(
            get("/v1/orders/{orderId}", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderNumber").value("PED-20260322-000123"))
        .andExpect(jsonPath("$.status").value("NEW"))
        .andExpect(jsonPath("$.customer.name").value("Lucas Santana"))
        .andExpect(jsonPath("$.address.street").value("Rua das Flores"))
        .andExpect(jsonPath("$.payment.paymentMethodSnapshot").value("PIX"))
        .andExpect(jsonPath("$.payment.paymentStatusSnapshot").value("PENDING"))
        .andExpect(jsonPath("$.items[0].productNameSnapshot").value("Pizza Calabresa"))
        .andExpect(jsonPath("$.items[0].options[0].optionNameSnapshot").value("Borda Catupiry"));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mockMvc
        .perform(get("/v1/orders/{orderId}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldForbidAdminOnTenantScopedRoute() throws Exception {
    mockMvc
        .perform(
            get("/v1/orders/{orderId}", UUID.randomUUID())
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
