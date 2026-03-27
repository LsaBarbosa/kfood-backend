package com.kfood.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ForwardedClientIpResolver implements ClientIpResolver {

  private static final String FALLBACK_IP = "0.0.0.0";
  private static final int MAX_IP_LENGTH = 45;
  private final boolean trustForwardedHeaders;

  public ForwardedClientIpResolver(
      @Value("${app.web.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
    this.trustForwardedHeaders = trustForwardedHeaders;
  }

  @Override
  public String resolve(HttpServletRequest request) {
    if (trustForwardedHeaders) {
      var forwardedFor = firstNonBlankToken(request.getHeader("X-Forwarded-For"));
      if (forwardedFor != null) {
        return forwardedFor;
      }

      var realIp = normalize(request.getHeader("X-Real-IP"));
      if (realIp != null) {
        return realIp;
      }
    }

    var remoteAddr = normalize(request.getRemoteAddr());
    return remoteAddr != null ? remoteAddr : FALLBACK_IP;
  }

  private String firstNonBlankToken(String value) {
    if (value == null) {
      return null;
    }
    for (var token : value.split(",")) {
      var normalized = normalize(token);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    var normalized = value.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    return normalized.length() <= MAX_IP_LENGTH
        ? normalized
        : normalized.substring(0, MAX_IP_LENGTH);
  }
}
