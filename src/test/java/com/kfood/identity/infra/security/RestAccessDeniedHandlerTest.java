package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.shared.exceptions.ApiErrorResponseFactory;
import com.kfood.shared.tenancy.TenantScopeAccessDeniedException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final RestAccessDeniedHandler handler =
      new RestAccessDeniedHandler(new ApiErrorResponseFactory());

  @Test
  void shouldWriteForbiddenJsonResponse() throws Exception {
    var request = new MockHttpServletRequest();
    request.setRequestURI("/v1/admin/\"audit\"\\logs");
    var response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("forbidden"));

    Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(body)
        .containsEntry("code", "AUTH_FORBIDDEN_ROLE")
        .containsEntry("message", "Authenticated user does not have permission for this resource.")
        .containsEntry("path", "/v1/admin/\"audit\"\\logs")
        .containsEntry("traceId", null);
    assertThat(body.get("timestamp")).isNotNull();
    assertThat(body.get("details")).isEqualTo(List.of());
  }

  @Test
  void shouldWriteTenantAccessDeniedJsonResponse() throws Exception {
    var request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/store");
    var response = new MockHttpServletResponse();

    handler.handle(
        request,
        response,
        new TenantScopeAccessDeniedException("Authenticated user cannot access another tenant."));

    Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(body)
        .containsEntry("code", "TENANT_ACCESS_DENIED")
        .containsEntry("message", "Authenticated user cannot access another tenant.")
        .containsEntry("path", "/v1/merchant/store")
        .containsEntry("traceId", null);
    assertThat(body.get("timestamp")).isNotNull();
    assertThat(body.get("details")).isEqualTo(List.of());
  }

  @Test
  void shouldUseDefaultTenantAccessDeniedMessageWhenExceptionMessageIsNull() throws Exception {
    var request = new MockHttpServletRequest();
    request.setRequestURI("/v1/merchant/store");
    var response = new MockHttpServletResponse();

    handler.handle(request, response, new TenantScopeAccessDeniedException(null));

    Map<String, Object> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(body.get("code")).isEqualTo("TENANT_ACCESS_DENIED");
    assertThat(body.get("message")).isEqualTo("Authenticated user cannot access another tenant.");
    assertThat(body.get("details")).isEqualTo(List.of());
  }
}
