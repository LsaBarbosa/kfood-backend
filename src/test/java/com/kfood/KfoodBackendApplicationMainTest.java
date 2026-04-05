package com.kfood;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class KfoodBackendApplicationMainTest {

  @Test
  void shouldExposePublicStaticMainMethod() throws NoSuchMethodException {
    var method = KfoodBackendApplication.class.getDeclaredMethod("main", String[].class);
    var modifiers = method.getModifiers();

    assertTrue(Modifier.isPublic(modifiers));
    assertTrue(Modifier.isStatic(modifiers));
  }

  @Test
  void shouldDelegateMainToSpringApplicationRun() {
    String[] args = {"--spring.main.web-application-type=none"};

    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      KfoodBackendApplication.main(args);

      springApplication.verify(() -> SpringApplication.run(KfoodBackendApplication.class, args));
    }
  }
}
