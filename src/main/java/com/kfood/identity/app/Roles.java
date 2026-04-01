package com.kfood.identity.app;

public final class Roles {

  public static final String ADMIN = "hasRole('ADMIN')";
  public static final String OWNER = "hasRole('OWNER')";
  public static final String OWNER_OR_ADMIN = "hasAnyRole('OWNER', 'ADMIN')";
  public static final String OWNER_OR_MANAGER = "hasAnyRole('OWNER', 'MANAGER')";
  public static final String OWNER_MANAGER_ATTENDANT =
      "hasAnyRole('OWNER', 'MANAGER', 'ATTENDANT')";

  private Roles() {}
}
