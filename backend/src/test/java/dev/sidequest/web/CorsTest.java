package dev.sidequest.web;

import dev.sidequest.support.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Originile permise în teste (application.properties din test):
 * http://localhost:5173 și pattern-ul https://sidequest-*.vercel.app.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class CorsTest {

    @Autowired MockMvc mvc;

    @Test
    void preflightFromAnAllowedOriginSucceeds() throws Exception {
        mvc.perform(options("/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void vercelPreviewDomainsMatchThePattern() throws Exception {
        mvc.perform(options("/groups/join")
                        .header("Origin", "https://sidequest-git-feature-x.vercel.app")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://sidequest-git-feature-x.vercel.app"));
    }

    @Test
    void otherOriginsAreRejected() throws Exception {
        mvc.perform(options("/users")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users/1/profile").header("Origin", "https://other-app.vercel.app"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsUpForRender() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
