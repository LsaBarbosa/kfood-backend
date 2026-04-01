package com.kfood.shared.exceptions;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp,
    String path,
    String traceId,
    List<ApiFieldError> details) {}
