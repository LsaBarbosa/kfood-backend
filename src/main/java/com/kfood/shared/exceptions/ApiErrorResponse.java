package com.kfood.shared.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp,
    String path,
    String traceId,
    List<ApiFieldError> details) {}
