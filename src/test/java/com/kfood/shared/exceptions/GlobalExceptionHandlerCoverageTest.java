package com.kfood.shared.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerCoverageTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void shouldHandleConstraintViolationAndIncludeTraceId() {
    HttpServletRequest request = request("/test-errors/constraint");
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);

    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn("create.name");
    when(violation.getMessage()).thenReturn("must not be blank");
    MDC.put("traceId", "trace-123");

    ResponseEntity<ApiErrorResponse> response =
        handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().message()).isEqualTo("Validation failed.");
    assertThat(response.getBody().path()).isEqualTo("/test-errors/constraint");
    assertThat(response.getBody().traceId()).isEqualTo("trace-123");
    assertThat(response.getBody().details())
        .containsExactly(new ApiFieldError("create.name", "must not be blank"));
  }

  @Test
  void shouldUseFallbackMessageWhenFieldValidationMessageIsNull() throws Exception {
    HttpServletRequest request = request("/test-errors/validation");
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
    bindingResult.addError(new FieldError("payload", "name", null, false, null, null, null));

    ResponseEntity<ApiErrorResponse> response =
        handler.handleMethodArgumentNotValid(
            new MethodArgumentNotValidException(methodParameter("handle"), bindingResult), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().details()).containsExactly(new ApiFieldError("name", "Invalid value."));
    assertThat(response.getBody().traceId()).isNull();
  }

  @Test
  void shouldReplaceNullBusinessDetailsAndBlankTraceId() {
    HttpServletRequest request = request("/test-errors/business");
    BusinessException exception =
        new BusinessException(ErrorCode.STORE_NOT_ACTIVE, "Store is not active.", HttpStatus.CONFLICT, null);
    MDC.put("traceId", "   ");

    ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("STORE_NOT_ACTIVE");
    assertThat(response.getBody().details()).isEmpty();
    assertThat(response.getBody().traceId()).isNull();
  }

  @Test
  void shouldMapNotFoundErrorResponseExceptionAndReuseProblemDetailMessage() {
    HttpServletRequest request = request("/missing");
    ErrorResponseException exception =
        new ErrorResponseException(
            HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource missing."), null);

    ResponseEntity<ApiErrorResponse> response = handler.handleErrorResponseException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(response.getBody().message()).isEqualTo("Resource missing.");
  }

  @Test
  void shouldMapForbiddenErrorResponseExceptionWhenProblemDetailHasNoDetail() {
    HttpServletRequest request = request("/forbidden");
    ErrorResponseException exception =
        new ErrorResponseException(HttpStatus.FORBIDDEN, ProblemDetail.forStatus(HttpStatus.FORBIDDEN), null);

    ResponseEntity<ApiErrorResponse> response = handler.handleErrorResponseException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("AUTH_FORBIDDEN_ROLE");
    assertThat(response.getBody().message()).isEqualTo("Request could not be processed.");
  }

  @Test
  void shouldMapUnauthorizedErrorResponseExceptionWhenProblemDetailIsBlank() {
    HttpServletRequest request = request("/unauthorized");
    ErrorResponseException exception =
        new ErrorResponseException(
            HttpStatus.UNAUTHORIZED,
            ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "   "),
            null);

    ResponseEntity<ApiErrorResponse> response = handler.handleErrorResponseException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
    assertThat(response.getBody().message()).isEqualTo("Request could not be processed.");
  }

  @Test
  void shouldMapUnexpectedErrorResponseExceptionWhenBodyIsNull() {
    HttpServletRequest request = request("/bad-request");
    ErrorResponseException exception = mock(ErrorResponseException.class);
    when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(exception.getBody()).thenReturn(null);

    ResponseEntity<ApiErrorResponse> response = handler.handleErrorResponseException(exception, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(response.getBody().message()).isEqualTo("Request could not be processed.");
    assertThat(response.getBody().details()).isEqualTo(List.of());
  }

  @Test
  void shouldBuildResponseWithEmptyDetailsWhenNullIsProvided() throws Exception {
    Method method =
        GlobalExceptionHandler.class.getDeclaredMethod(
            "buildResponse",
            ErrorCode.class,
            String.class,
            HttpServletRequest.class,
            List.class);
    method.setAccessible(true);

    ApiErrorResponse response =
        (ApiErrorResponse)
            method.invoke(
                handler, ErrorCode.UNEXPECTED_ERROR, "An unexpected error occurred.", request("/oops"), null);

    assertThat(response.details()).isEmpty();
    assertThat(response.path()).isEqualTo("/oops");
  }

  private HttpServletRequest request(String uri) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    return request;
  }

  private MethodParameter methodParameter(String methodName) throws Exception {
    Method method = ValidationTarget.class.getDeclaredMethod(methodName, ValidationPayload.class);
    return new MethodParameter(method, 0);
  }

  static class ValidationTarget {

    @SuppressWarnings("unused")
    void handle(ValidationPayload payload) {}
  }

  static class ValidationPayload {}
}
