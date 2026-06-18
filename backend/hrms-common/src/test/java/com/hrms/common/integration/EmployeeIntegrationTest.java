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
 * Integration tests for the Employee CRUD endpoints.
 */
@SpringBootTest(
        classes = TestHrmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Employee Integration Tests")
class EmployeeIntegrationTest {

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:hrms_emp_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("JWT_SECRET", () -> "0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("HRMS_AES_KEY", () -> "12345678901234567890123456789012");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String EMPLOYEES_URL = "/api/hr/employees";

    // ─── list_employees ──────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("list_employees: GET /api/hr/employees → 200 + paged data")
    void list_employees() throws Exception {
        String token = loginAs("admin", "Admin@2026");

        mockMvc.perform(get(EMPLOYEES_URL)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(2))));
    }

    // ─── get_employee_detail ─────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("get_employee_detail: GET /api/hr/employees/{id} → 200 + detail")
    void get_employee_detail() throws Exception {
        String token = loginAs("admin", "Admin@2026");

        // Seed employee id=1001, emp_no=E001001, name=张三
        mockMvc.perform(get(EMPLOYEES_URL + "/1001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.employee").isNotEmpty())
                .andExpect(jsonPath("$.data.employee.name").value("张三"))
                .andExpect(jsonPath("$.data.employee.empNo").value("E001001"));
    }

    // ─── create_employee ─────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("create_employee: POST /api/hr/employees → 200")
    void create_employee() throws Exception {
        String token = loginAs("admin", "Admin@2026");

        mockMvc.perform(post(EMPLOYEES_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "王五",
                                  "gender": "M",
                                  "deptId": 202,
                                  "positionId": 403,
                                  "hireDate": "2025-06-01",
                                  "email": "wangwu@example.com",
                                  "phone": "13800000001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("王五"));
    }

    // ─── update_employee ─────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("update_employee: PUT /api/hr/employees/{id} → 200")
    void update_employee() throws Exception {
        String token = loginAs("admin", "Admin@2026");

        // Update seed employee id=1002 (李四) – change email
        mockMvc.perform(put(EMPLOYEES_URL + "/1002")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "lisi_updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("lisi_updated@example.com"));
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
