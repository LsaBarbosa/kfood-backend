package com.kfood.shared.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ApiFieldError> details =
        ex.getBindingResult().getFieldErrors().stream().map(this::mapFieldError).toList();

    ApiErrorResponse response =
        buildResponse(ErrorCode.VALIDATION_ERROR, "Validation failed.", request, details);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<ApiFieldError> details =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    new ApiFieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

    ApiErrorResponse response =
        buildResponse(ErrorCode.VALIDATION_ERROR, "Validation failed.", request, details);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusinessException(
      BusinessException ex, HttpServletRequest request) {
    ApiErrorResponse response =
        buildResponse(ex.getErrorCode(), ex.getMessage(), request, ex.getDetails());

    return ResponseEntity.status(ex.getStatus()).body(response);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ApiErrorResponse response =
        buildResponse(
            ErrorCode.AUTH_FORBIDDEN_ROLE,
            "Authenticated user does not have permission for this resource.",
            request,
            List.of());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ApiErrorResponse> handleErrorResponseException(
      ErrorResponseException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

    String message = extractSafeMessage(ex);
    ErrorCode errorCode = mapHttpStatusToDefaultCode(status);

    ApiErrorResponse response = buildResponse(errorCode, message, request, List.of());

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
      Exception ex, HttpServletRequest request) {
    ApiErrorResponse response =
        buildResponse(
            ErrorCode.UNEXPECTED_ERROR, "An unexpected error occurred.", request, List.of());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  private ApiFieldError mapFieldError(FieldError fieldError) {
    return new ApiFieldError(
        fieldError.getField(),
        fieldError.getDefaultMessage() == null ? "Invalid value." : fieldError.getDefaultMessage());
  }

  private ApiErrorResponse buildResponse(
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
        details == null ? List.of() : details);
  }

  private String resolveTraceId() {
    String traceId = MDC.get("traceId");
    return traceId == null || traceId.isBlank() ? null : traceId;
  }

  private String extractSafeMessage(ErrorResponseException ex) {
    ProblemDetail body = ex.getBody();
    if (body != null && body.getDetail() != null && !body.getDetail().isBlank()) {
      return body.getDetail();
    }
    return "Request could not be processed.";
  }

  private ErrorCode mapHttpStatusToDefaultCode(HttpStatus status) {
    return switch (status) {
      case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
      case FORBIDDEN -> ErrorCode.AUTH_FORBIDDEN_ROLE;
      case UNAUTHORIZED -> ErrorCode.AUTH_INVALID_CREDENTIALS;
      default -> ErrorCode.UNEXPECTED_ERROR;
    };
  }
}
