package com.kfood.shared.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.shared.tenancy.TenantScopeAccessDeniedException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

  private static final String DEFAULT_VALIDATION_MESSAGE = "Validation failed.";
  private static final String DEFAULT_INVALID_CREDENTIALS_MESSAGE =
      "Authentication is required or token is invalid.";
  private static final String DEFAULT_EXPIRED_TOKEN_MESSAGE = "Authentication token has expired.";
  private static final String DEFAULT_FORBIDDEN_ROLE_MESSAGE =
      "Authenticated user does not have permission for this resource.";
  private static final String DEFAULT_TENANT_ACCESS_DENIED_MESSAGE =
      "Authenticated user cannot access another tenant.";

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  public ApiErrorResponse create(
      ErrorCode errorCode,
      String message,
      HttpServletRequest request,
      List<ApiFieldError> details) {
    return new ApiErrorResponse(
        errorCode.name(),
        message,
        OffsetDateTime.now(),
        request.getRequestURI(),
        resolveTraceId(),
        details == null ? List.of() : List.copyOf(details));
  }

  public ApiErrorResponse createValidationError(
      HttpServletRequest request, List<ApiFieldError> details) {
    return create(ErrorCode.VALIDATION_ERROR, DEFAULT_VALIDATION_MESSAGE, request, details);
  }

  public ApiErrorResponse createAuthenticationError(
      HttpServletRequest request, AuthenticationException authenticationException) {
    return create(
        resolveAuthenticationErrorCode(authenticationException),
        resolveAuthenticationMessage(authenticationException),
        request,
        List.of());
  }

  public ApiErrorResponse createAccessDeniedError(
      HttpServletRequest request, AccessDeniedException accessDeniedException) {
    return create(
        resolveAccessDeniedErrorCode(accessDeniedException),
        resolveAccessDeniedMessage(accessDeniedException),
        request,
        List.of());
  }

  public void write(
      HttpServletResponse response, HttpStatus status, ApiErrorResponse apiErrorResponse)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    LinkedHashMap<String, Object> body = new LinkedHashMap<>();
    body.put("code", apiErrorResponse.code());
    body.put("message", apiErrorResponse.message());
    body.put("timestamp", apiErrorResponse.timestamp().toString());
    body.put("path", apiErrorResponse.path());
    body.put("details", apiErrorResponse.details());
    body.put("traceId", apiErrorResponse.traceId());
    objectMapper.writeValue(response.getWriter(), body);
  }

  public ErrorCode resolveAuthenticationErrorCode(AuthenticationException authenticationException) {
    if (authenticationException instanceof CredentialsExpiredException
        || authenticationException.getCause() instanceof ExpiredJwtException) {
      return ErrorCode.AUTH_TOKEN_EXPIRED;
    }
    return ErrorCode.AUTH_INVALID_CREDENTIALS;
  }

  public String resolveAuthenticationMessage(AuthenticationException authenticationException) {
    if (resolveAuthenticationErrorCode(authenticationException) == ErrorCode.AUTH_TOKEN_EXPIRED) {
      return DEFAULT_EXPIRED_TOKEN_MESSAGE;
    }
    return DEFAULT_INVALID_CREDENTIALS_MESSAGE;
  }

  public ErrorCode resolveAccessDeniedErrorCode(AccessDeniedException accessDeniedException) {
    if (accessDeniedException instanceof TenantScopeAccessDeniedException) {
      return ErrorCode.TENANT_ACCESS_DENIED;
    }
    return ErrorCode.AUTH_FORBIDDEN_ROLE;
  }

  public String resolveAccessDeniedMessage(AccessDeniedException accessDeniedException) {
    if (accessDeniedException instanceof TenantScopeAccessDeniedException) {
      return accessDeniedException.getMessage() == null
              || accessDeniedException.getMessage().isBlank()
          ? DEFAULT_TENANT_ACCESS_DENIED_MESSAGE
          : accessDeniedException.getMessage();
    }
    return DEFAULT_FORBIDDEN_ROLE_MESSAGE;
  }

  private String resolveTraceId() {
    String traceId = MDC.get("traceId");
    return traceId == null || traceId.isBlank() ? null : traceId;
  }
}
