package com.kfood.shared.exceptions;

import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.identity.app.JwtTokenReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GlobalExceptionHandlerTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
  GlobalExceptionHandler.class,
  ApiErrorResponseFactory.class,
  GlobalExceptionHandlerWebMvcTest.MockConfig.class
})
class GlobalExceptionHandlerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("should return standardized payload for validation exception")
  void shouldReturnStandardizedPayloadForValidationException() throws Exception {
    mockMvc
        .perform(
            post("/test-errors/validation")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                {
                                  "name": ""
                                }
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Validation failed."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/test-errors/validation"))
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.details[0].field").value("name"))
        .andExpect(jsonPath("$.details[0].message").value("name must not be blank"));
  }

  @Test
  @DisplayName("should return code and message for business exception")
  void shouldReturnCodeAndMessageForBusinessException() throws Exception {
    mockMvc
        .perform(get("/test-errors/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STORE_NOT_ACTIVE"))
        .andExpect(jsonPath("$.message").value("Store is not active."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/test-errors/business"))
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("should return standardized payload for resource not found")
  void shouldReturnStandardizedPayloadForResourceNotFound() throws Exception {
    mockMvc
        .perform(get("/test-errors/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Request could not be processed."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/test-errors/not-found"))
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("should return standardized payload for conflict")
  void shouldReturnStandardizedPayloadForConflict() throws Exception {
    mockMvc
        .perform(get("/test-errors/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Phone and email belong to different customers."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/test-errors/conflict"))
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("should return tenant access denied code for tenant violations")
  void shouldReturnTenantAccessDeniedCodeForTenantViolations() throws Exception {
    mockMvc
        .perform(get("/test-errors/tenant-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"))
        .andExpect(jsonPath("$.message").value("Authenticated user cannot access another tenant."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/test-errors/tenant-denied"))
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("should not leak stacktrace for unexpected exception")
  void shouldNotLeakStacktraceForUnexpectedException() throws Exception {
    mockMvc
        .perform(get("/test-errors/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("UNEXPECTED_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/test-errors/unexpected"))
        .andExpect(jsonPath("$.details").isEmpty())
        .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("IllegalStateException"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database exploded"))));
  }

  @Test
  @DisplayName("should include timestamp and path when applicable")
  void shouldIncludeTimestampAndPathWhenApplicable() throws Exception {
    mockMvc
        .perform(get("/test-errors/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.path").value("/test-errors/business"));
  }

  @TestConfiguration
  static class MockConfig {

    @Bean
    JwtTokenReader jwtTokenReader() {
      return mock(JwtTokenReader.class);
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
      return mock(AuthenticationEntryPoint.class);
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
      return mock(AccessDeniedHandler.class);
    }
  }
}
