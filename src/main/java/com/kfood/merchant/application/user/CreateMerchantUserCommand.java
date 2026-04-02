package com.kfood.merchant.application.user;

import com.kfood.identity.domain.UserRoleName;
import com.kfood.identity.domain.UserStatus;
import java.util.Set;

public record CreateMerchantUserCommand(
    String email, String temporaryPassword, Set<UserRoleName> roles, UserStatus status) {}
