package com.kfood.shared.web;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpResolver {

  String resolve(HttpServletRequest request);
}
