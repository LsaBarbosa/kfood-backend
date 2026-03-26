package com.kfood.shared.exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-errors")
@Validated
class GlobalExceptionHandlerTestController {

  @PostMapping("/validation")
  public String validation(@Valid @RequestBody TestValidationRequest request) {
    return "ok";
  }

  @GetMapping("/business")
  public String business() {
    throw new BusinessException(
        ErrorCode.STORE_NOT_ACTIVE, "Store is not active.", HttpStatus.CONFLICT);
  }

  @GetMapping("/conflict")
  public String conflict() {
    throw new BusinessException(
        ErrorCode.VALIDATION_ERROR, "Phone and email belong to different customers.", HttpStatus.CONFLICT);
  }

  @GetMapping("/not-found")
  public String notFound() {
    throw new ErrorResponseException(HttpStatus.NOT_FOUND);
  }

  @GetMapping("/tenant-denied")
  public String tenantDenied() {
    throw new com.kfood.shared.tenancy.TenantScopeAccessDeniedException(
        "Authenticated user cannot access another tenant.");
  }

  @GetMapping("/unexpected")
  public String unexpected() {
    throw new IllegalStateException("database exploded with stacktrace details");
  }

  record TestValidationRequest(@NotBlank(message = "name must not be blank") String name) {}
}
