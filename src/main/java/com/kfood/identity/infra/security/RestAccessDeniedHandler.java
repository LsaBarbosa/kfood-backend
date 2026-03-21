package com.kfood.identity.infra.security;

import com.kfood.shared.exceptions.ErrorCode;
import com.kfood.shared.tenancy.TenantScopeAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    var body =
        """
        {
          "code":"%s",
          "message":"%s",
          "timestamp":"%s",
          "path":"%s"
        }
        """
            .formatted(
                codeOf(accessDeniedException),
                escapeJson(messageOf(accessDeniedException)),
                OffsetDateTime.now(),
                escapeJson(request.getRequestURI()));

    response.getWriter().write(body);
  }

  private String codeOf(AccessDeniedException accessDeniedException) {
    if (accessDeniedException instanceof TenantScopeAccessDeniedException) {
      return ErrorCode.TENANT_ACCESS_DENIED.name();
    }
    return ErrorCode.AUTH_FORBIDDEN_ROLE.name();
  }

  private String messageOf(AccessDeniedException accessDeniedException) {
    if (accessDeniedException instanceof TenantScopeAccessDeniedException) {
      return accessDeniedException.getMessage() == null
          ? "Authenticated user cannot access another tenant."
          : accessDeniedException.getMessage();
    }
    return "Authenticated user does not have permission for this resource.";
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
