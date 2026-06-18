package com.hrms.common.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Department CRUD and tree endpoints.
 */
@SpringBootTest(
        classes = TestHrmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Department Integration Tests")
class DepartmentIntegrationTest {

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:hrms_dept_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("JWT_SECRET", () -> "0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("HRMS_AES_KEY", () -> "12345678901234567890123456789012");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String DEPARTMENTS_URL = "/api/departments";

    // ─── get_org_tree ────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("get_org_tree: GET /api/departments/tree?companyId=100 → 200")
    void get_org_tree() throws Exception {
        String token = loginAs("admin", "Admin@2026");

        // Seed data has 3 departments for companyId=100 (HR, RD, SALES)
        mockMvc.perform(get(DEPARTMENTS_URL + "/tree")
                        .header("Authorization", "Bearer " + token)
                        .param("companyId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))));
    }

    // ─── create_department ───────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("create_department: POST /api/departments → 200")
    void create_department() throws Exception {
        String token = loginAs("admin", "Admin@2026");

        mockMvc.perform(post(DEPARTMENTS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 100,
                                  "name": "财务部",
                                  "code": "FIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("财务部"))
                .andExpect(jsonPath("$.data.code").value("FIN"));
    }

    // ─── helpers ──────────────────────────────────────────────────────

    /**
     * Perform a login and return the accessToken.
     */
    private String loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }
}
