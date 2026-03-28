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
import com.kfood.payment.app.PaymentNotFoundException;
import com.kfood.payment.app.UpdatePaymentStatusCommand;
import com.kfood.payment.app.UpdatePaymentStatusOutput;
import com.kfood.payment.app.UpdatePaymentStatusUseCase;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
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
class OrderPaymentStatusControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenService jwtTokenService;

  @MockitoBean private UpdatePaymentStatusUseCase updatePaymentStatusUseCase;

  @Test
  void shouldMarkPaymentAsConfirmedAndOrderAsPaid() throws Exception {
    var paymentId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(updatePaymentStatusUseCase.execute(eq(new UpdatePaymentStatusCommand(paymentId, PaymentStatus.CONFIRMED))))
        .thenReturn(
            new UpdatePaymentStatusOutput(
                paymentId, orderId, PaymentStatus.CONFIRMED, PaymentStatusSnapshot.PAID));

    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", paymentId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"))
        .andExpect(jsonPath("$.paymentStatusSnapshot").value("PAID"));
  }

  @Test
  void shouldMarkPaymentAsFailedAndOrderAsFailed() throws Exception {
    var paymentId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    when(updatePaymentStatusUseCase.execute(eq(new UpdatePaymentStatusCommand(paymentId, PaymentStatus.FAILED))))
        .thenReturn(
            new UpdatePaymentStatusOutput(
                paymentId, orderId, PaymentStatus.FAILED, PaymentStatusSnapshot.FAILED));

    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", paymentId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.OWNER))
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"FAILED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
        .andExpect(jsonPath("$.paymentStatusSnapshot").value("FAILED"));
  }

  @Test
  void shouldReturnConflictWhenPaymentTransitionIsInvalid() throws Exception {
    var paymentId = UUID.randomUUID();
    when(updatePaymentStatusUseCase.execute(any(UpdatePaymentStatusCommand.class)))
        .thenThrow(
            new BusinessException(
                ErrorCode.PAYMENT_STATUS_TRANSITION_INVALID,
                "Invalid payment status transition from CONFIRMED to FAILED",
                HttpStatus.CONFLICT));

    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", paymentId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"FAILED\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PAYMENT_STATUS_TRANSITION_INVALID"))
        .andExpect(
            jsonPath("$.message").value("Invalid payment status transition from CONFIRMED to FAILED"));
  }

  @Test
  void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
    var paymentId = UUID.randomUUID();
    when(updatePaymentStatusUseCase.execute(any(UpdatePaymentStatusCommand.class)))
        .thenThrow(new PaymentNotFoundException(paymentId));

    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", paymentId)
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMED\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMED\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void shouldForbidAttendantForOperationalPaymentStatusUpdate() throws Exception {
    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.ATTENDANT))
                .contentType(APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMED\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN_ROLE"));
  }

  @Test
  void shouldRejectInvalidRequestBody() throws Exception {
    mockMvc
        .perform(
            patch("/v1/orders/payments/{paymentId}/status", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenOf(UserRoleName.MANAGER))
                .contentType(APPLICATION_JSON)
                .content("{\"other\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("newStatus"));
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
