package com.kfood;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class KfoodBackendApplicationMainTest {

  @Test
  void shouldDelegateMainToSpringApplicationRun() {
    String[] args = {"--spring.main.web-application-type=none"};

    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      KfoodBackendApplication.main(args);

      springApplication.verify(() -> SpringApplication.run(KfoodBackendApplication.class, args));
    }
  }
}
