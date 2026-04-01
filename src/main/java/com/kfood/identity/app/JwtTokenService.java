package com.kfood.identity.app;

import com.kfood.identity.persistence.IdentityUserEntity;

public interface JwtTokenService {

  String generateToken(IdentityUserEntity user);
}
