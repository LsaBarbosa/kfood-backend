package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.shared.exceptions.ApiErrorResponseFactory;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

class RestAuthenticationEntryPointTest {

  private final RestAuthenticationEntryPoint entryPoint =
      new RestAuthenticationEntryPoint(new ApiErrorResponseFactory());
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void shouldWriteUnauthorizedJsonResponseWhenRequestHasNoToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(
        request, response, new BadCredentialsException("Authentication is required."));

    Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(body)
        .containsEntry("code", "AUTH_INVALID_CREDENTIALS")
        .containsEntry("message", "Authentication is required or token is invalid.")
        .containsEntry("path", "/v1/merchant/me")
        .containsEntry("traceId", null);
    assertThat(body.get("timestamp")).isNotNull();
    assertThat(body.get("details")).isEqualTo(java.util.List.of());
  }

  @Test
  void shouldWriteUnauthorizedJsonResponseWhenTokenIsInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new BadCredentialsException("Invalid token."));

    Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(body.get("code")).isEqualTo("AUTH_INVALID_CREDENTIALS");
    assertThat(body.get("message")).isEqualTo("Authentication is required or token is invalid.");
    assertThat(body.get("details")).isEqualTo(java.util.List.of());
  }

  @Test
  void shouldWriteUnauthorizedJsonResponseWhenTokenIsExpired() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/me");
    MockHttpServletResponse response = new MockHttpServletResponse();
    ExpiredJwtException expiredJwtException =
        new ExpiredJwtException(null, Jwts.claims().subject("owner@kfood.local").build(), "expired");

    entryPoint.commence(
        request,
        response,
        new CredentialsExpiredException("Expired token.", expiredJwtException));

    Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(body.get("code")).isEqualTo("AUTH_TOKEN_EXPIRED");
    assertThat(body.get("message")).isEqualTo("Authentication token has expired.");
    assertThat(body.get("details")).isEqualTo(java.util.List.of());
  }
}
