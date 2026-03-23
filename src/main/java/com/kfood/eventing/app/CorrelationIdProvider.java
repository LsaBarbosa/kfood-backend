package com.kfood.eventing.app;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdProvider {

  public String getOrCreate() {
    var traceId = MDC.get("traceId");
    if (traceId != null && !traceId.isBlank()) {
      return traceId;
    }

    return UUID.randomUUID().toString();
  }
}
