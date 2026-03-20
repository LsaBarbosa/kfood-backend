package com.kfood.identity.infra.security;

import com.kfood.shared.exceptions.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
                ErrorCode.AUTH_INVALID_CREDENTIALS.name(),
                "Authentication is required or token is invalid.",
                OffsetDateTime.now(),
                escapeJson(request.getRequestURI()));

    response.getWriter().write(body);
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
