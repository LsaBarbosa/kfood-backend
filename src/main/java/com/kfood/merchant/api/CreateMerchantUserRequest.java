package com.kfood.merchant.api;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateMerchantUserRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72) String temporaryPassword,
    @NotEmpty Set<UserRoleName> roles,
    @NotNull UserStatus status) {}
