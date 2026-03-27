package com.kfood.merchant.application.user;

import com.kfood.identity.domain.UserRoleName;
import java.util.Set;

public record CreateMerchantUserCommand(String email, String password, Set<UserRoleName> roles) {}
