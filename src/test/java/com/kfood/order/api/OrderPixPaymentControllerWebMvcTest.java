package com.kfood.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.payment.app.CreateOrderPixPaymentCommand;
import com.kfood.payment.app.CreateOrderPixPaymentUseCase;
import com.kfood.payment.app.OrderPixPaymentOutput;
import com.kfood.payment.app.gateway.PaymentGatewayErrorType;
import com.kfood.payment.app.gateway.PaymentGatewayException;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class OrderPixPaymentControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreateOrderPixPaymentUseCase createOrderPixPaymentUseCase;

  @Test
  void shouldCreatePixPaymentWhenHeaderIsPresent() throws Exception {
    var orderId = UUID.randomUUID();
    var paymentId = UUID.randomUUID();
    when(createOrderPixPaymentUseCase.execute(any(CreateOrderPixPaymentCommand.class)))
        .thenReturn(
            new OrderPixPaymentOutput(
                paymentId,
                orderId,
                PaymentMethod.PIX,
                PaymentStatus.PENDING,
                "pix-ref-123",
                "000201mock",
                OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .header("Idempotency-Key", "idem-123")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 57.50,
                      "provider": "mock"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.paymentMethod").value("PIX"))
        .andExpect(jsonPath("$.paymentMethodSnapshot").value("PIX"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.technicalPaymentStatus").value("PENDING"))
        .andExpect(jsonPath("$.paymentStatusSnapshot").value("PENDING"))
        .andExpect(jsonPath("$.providerReference").value("pix-ref-123"))
        .andExpect(jsonPath("$.qrCodePayload").value("000201mock"))
        .andExpect(jsonPath("$.expiresAt").value("2099-01-01T00:30:00Z"));

    ArgumentCaptor<CreateOrderPixPaymentCommand> captor =
        ArgumentCaptor.forClass(CreateOrderPixPaymentCommand.class);
    verify(createOrderPixPaymentUseCase).execute(captor.capture());
    assertThat(captor.getValue().idempotencyKey()).isEqualTo("idem-123");
  }

  @Test
  void shouldCreatePixPaymentWhenHeaderIsAbsent() throws Exception {
    var orderId = UUID.randomUUID();
    when(createOrderPixPaymentUseCase.execute(any(CreateOrderPixPaymentCommand.class)))
        .thenReturn(
            new OrderPixPaymentOutput(
                UUID.randomUUID(),
                orderId,
                PaymentMethod.PIX,
                PaymentStatus.PENDING,
                "pix-ref-123",
                "000201mock",
                OffsetDateTime.parse("2099-01-01T00:30:00Z")));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 57.50,
                      "provider": "mock"
                    }
                    """))
        .andExpect(status().isCreated());

    ArgumentCaptor<CreateOrderPixPaymentCommand> captor =
        ArgumentCaptor.forClass(CreateOrderPixPaymentCommand.class);
    verify(createOrderPixPaymentUseCase).execute(captor.capture());
    assertThat(captor.getValue().idempotencyKey()).isNull();
  }

  @Test
  void shouldReturnStandardizedErrorWhenProviderFails() throws Exception {
    var orderId = UUID.randomUUID();
    when(createOrderPixPaymentUseCase.execute(any(CreateOrderPixPaymentCommand.class)))
        .thenThrow(
            new PaymentGatewayException(
                "mock", PaymentGatewayErrorType.PROVIDER_UNAVAILABLE, "Pix provider unavailable"));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 57.50,
                      "provider": "mock"
                    }
                    """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Pix provider unavailable"))
        .andExpect(jsonPath("$.path").value("/v1/orders/" + orderId + "/payments/pix"));
  }

  @Test
  void shouldReturnStandardizedErrorWhenProviderResponseIsInvalid() throws Exception {
    var orderId = UUID.randomUUID();
    when(createOrderPixPaymentUseCase.execute(any(CreateOrderPixPaymentCommand.class)))
        .thenThrow(
            new PaymentGatewayException(
                "mock",
                PaymentGatewayErrorType.INVALID_REQUEST,
                "Pix provider returned invalid response"));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 57.50,
                      "provider": "mock"
                    }
                    """))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_INVALID_RESPONSE"))
        .andExpect(jsonPath("$.message").value("Pix provider returned invalid response"))
        .andExpect(jsonPath("$.path").value("/v1/orders/" + orderId + "/payments/pix"));
  }

  @Test
  void shouldReturnStandardizedErrorWhenProviderIsUnsupported() throws Exception {
    var orderId = UUID.randomUUID();
    when(createOrderPixPaymentUseCase.execute(any(CreateOrderPixPaymentCommand.class)))
        .thenThrow(
            new PaymentGatewayException(
                "legacy",
                PaymentGatewayErrorType.PROVIDER_NOT_SUPPORTED,
                "Payment provider is not supported: legacy"));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 57.50,
                      "provider": "legacy"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_NOT_SUPPORTED"))
        .andExpect(jsonPath("$.message").value("Payment provider is not supported: legacy"))
        .andExpect(jsonPath("$.path").value("/v1/orders/" + orderId + "/payments/pix"));
  }

  @Test
  void shouldReturnStandardizedErrorWhenProviderTimesOut() throws Exception {
    var orderId = UUID.randomUUID();
    when(createOrderPixPaymentUseCase.execute(any(CreateOrderPixPaymentCommand.class)))
        .thenThrow(
            new PaymentGatewayException(
                "mock", PaymentGatewayErrorType.TIMEOUT, "Pix provider timed out"));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 57.50,
                      "provider": "mock"
                    }
                    """))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_TIMEOUT"))
        .andExpect(jsonPath("$.message").value("Pix provider timed out"))
        .andExpect(jsonPath("$.path").value("/v1/orders/" + orderId + "/payments/pix"));
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
