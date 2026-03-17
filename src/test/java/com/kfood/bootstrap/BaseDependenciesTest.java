package com.kfood.bootstrap;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BaseDependenciesTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadMainBeansIntoContext() {
        assertThat(applicationContext.getBeansOfType(RequestMappingHandlerMapping.class)).isNotEmpty();
        assertThat(applicationContext.getBeansOfType(Validator.class)).isNotEmpty();
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain.class)).isNotEmpty();
        assertThat(applicationContext.getBeansOfType(HealthEndpoint.class)).isNotEmpty();
        assertThat(
            ClassUtils.isPresent(
                "org.springframework.data.jpa.repository.JpaRepository",
                getClass().getClassLoader()
            )
        ).isTrue();
    }

    @Test
    void shouldExposeHealthEndpointWithoutConflicts() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
