package com.kfood.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200WhenCallingPing() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldReturnExpectedPayloadWhenCallingPing() throws Exception {
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldReturn405WhenUsingInvalidHttpMethodOnPing() throws Exception {
        mockMvc.perform(post("/ping"))
            .andExpect(status().isMethodNotAllowed());
    }
}
