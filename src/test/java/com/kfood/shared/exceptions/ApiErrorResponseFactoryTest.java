package com.kfood.shared.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

class ApiErrorResponseFactoryTest {

  private final ApiErrorResponseFactory factory = new ApiErrorResponseFactory();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void shouldBuildErrorPayloadWithAllExpectedFields() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/orders");
    MDC.put("traceId", "trace-123");

    ApiErrorResponse response =
        factory.create(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Order not found.",
            request,
            List.of(new ApiFieldError("id", "must exist")));

    assertThat(response.code()).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(response.message()).isEqualTo("Order not found.");
    assertThat(response.path()).isEqualTo("/v1/orders");
    assertThat(response.timestamp()).isNotNull();
    assertThat(response.details()).containsExactly(new ApiFieldError("id", "must exist"));
    assertThat(response.traceId()).isEqualTo("trace-123");
  }

  @Test
  void shouldBuildAuthenticationPayloadForInvalidCredentials() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/me");

    ApiErrorResponse response =
        factory.createAuthenticationError(
            request, new BadCredentialsException("Invalid authorization header."));

    assertThat(response.code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
    assertThat(response.message()).isEqualTo("Authentication is required or token is invalid.");
    assertThat(response.details()).isEmpty();
    assertThat(response.traceId()).isNull();
  }

  @Test
  void shouldBuildAuthenticationPayloadForExpiredToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/me");
    ExpiredJwtException expiredJwtException =
        new ExpiredJwtException(
            null, Jwts.claims().subject("owner@kfood.local").build(), "expired");

    ApiErrorResponse response =
        factory.createAuthenticationError(
            request, new CredentialsExpiredException("Expired token.", expiredJwtException));

    assertThat(response.code()).isEqualTo("AUTH_TOKEN_EXPIRED");
    assertThat(response.message()).isEqualTo("Authentication token has expired.");
    assertThat(response.details()).isEmpty();
  }

  @Test
  void shouldBuildAccessDeniedPayloadsForRoleAndTenantViolations() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/store");

    ApiErrorResponse roleResponse =
        factory.createAccessDeniedError(request, new AccessDeniedException("forbidden"));
    ApiErrorResponse tenantResponse =
        factory.createAccessDeniedError(
            request,
            new com.kfood.shared.tenancy.TenantScopeAccessDeniedException(
                "Authenticated user cannot access another tenant."));

    assertThat(roleResponse.code()).isEqualTo("AUTH_FORBIDDEN_ROLE");
    assertThat(roleResponse.message())
        .isEqualTo("Authenticated user does not have permission for this resource.");
    assertThat(tenantResponse.code()).isEqualTo("TENANT_ACCESS_DENIED");
    assertThat(tenantResponse.message())
        .isEqualTo("Authenticated user cannot access another tenant.");
  }

  @Test
  void shouldUseDefaultTenantMessageWhenAccessDeniedMessageIsBlank() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/store");

    ApiErrorResponse response =
        factory.createAccessDeniedError(
            request, new com.kfood.shared.tenancy.TenantScopeAccessDeniedException("   "));

    assertThat(response.code()).isEqualTo("TENANT_ACCESS_DENIED");
    assertThat(response.message()).isEqualTo("Authenticated user cannot access another tenant.");
  }
}
