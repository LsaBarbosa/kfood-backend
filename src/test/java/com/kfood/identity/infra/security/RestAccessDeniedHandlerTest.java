package com.kfood.identity.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

  private final RestAccessDeniedHandler handler = new RestAccessDeniedHandler();

  @Test
  void shouldWriteForbiddenJsonResponse() throws Exception {
    var request = new MockHttpServletRequest();
    request.setRequestURI("/v1/admin/\"audit\"\\logs");
    var response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("forbidden"));

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(response.getContentAsString()).contains("AUTH_FORBIDDEN_ROLE");
    assertThat(response.getContentAsString())
        .contains("Authenticated user does not have permission for this resource.");
    assertThat(response.getContentAsString()).contains("/v1/admin/\\\"audit\\\"\\\\logs");
  }
}
