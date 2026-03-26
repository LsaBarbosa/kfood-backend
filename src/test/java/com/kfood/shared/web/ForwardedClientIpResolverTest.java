package com.kfood.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ForwardedClientIpResolverTest {

  private final ForwardedClientIpResolver resolver = new ForwardedClientIpResolver();

  @Test
  void shouldResolveIpFromForwardedHeaderBeforeRemoteAddr() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "198.51.100.10, 10.0.0.1");
    request.setRemoteAddr("10.0.0.1");

    assertThat(resolver.resolve(request)).isEqualTo("198.51.100.10");
  }

  @Test
  void shouldFallbackToRemoteAddrWhenForwardedHeadersAreMissing() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("203.0.113.7");

    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
  }

  @Test
  void shouldFallbackToDefaultWhenRequestHasNoIpData() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn(null);

    assertThat(resolver.resolve(request)).isEqualTo("0.0.0.0");
  }
}
