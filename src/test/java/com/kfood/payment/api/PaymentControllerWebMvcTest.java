package com.kfood.payment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenService;
import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import com.kfood.identity.persistence.IdentityUserEntity;
import com.kfood.payment.app.CreatePixPaymentCommand;
import com.kfood.payment.app.CreatePixPaymentUseCase;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.OffsetDateTime;
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
class PaymentControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private CreatePixPaymentUseCase createPixPaymentUseCase;

  @Test
  void shouldReturnCreatedPixPayment() throws Exception {
    var orderId = UUID.randomUUID();
    var paymentId = UUID.randomUUID();
    when(createPixPaymentUseCase.execute(any(CreatePixPaymentCommand.class)))
        .thenReturn(
            new CreatePixPaymentResponse(
                paymentId,
                orderId,
                PaymentMethod.PIX,
                PaymentStatus.PENDING,
                "psp_100",
                "0002012636mockpix100",
                OffsetDateTime.parse("2026-03-22T12:15:00Z")));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .header("Idempotency-Key", "idem-123")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 56.50,
                      "provider": "default-psp"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.paymentMethod").value("PIX"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.providerReference").value("psp_100"))
        .andExpect(jsonPath("$.qrCodePayload").value("0002012636mockpix100"))
        .andExpect(jsonPath("$.expiresAt").value("2026-03-22T12:15:00Z"));
  }

  @Test
  void shouldReturnControlledProviderFailure() throws Exception {
    var orderId = UUID.randomUUID();
    when(createPixPaymentUseCase.execute(any(CreatePixPaymentCommand.class)))
        .thenThrow(
            new BusinessException(
                ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE,
                "Payment provider is unavailable.",
                HttpStatus.SERVICE_UNAVAILABLE));

    mockMvc
        .perform(
            post("/v1/orders/{orderId}/payments/pix", orderId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "amount": 56.50,
                      "provider": "default-psp"
                    }
                    """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Payment provider is unavailable."));
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
