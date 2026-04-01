package com.kfood.identity.app;

public interface PasswordHashService {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String passwordHash);
}
