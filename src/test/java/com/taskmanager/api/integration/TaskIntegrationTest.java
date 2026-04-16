package com.taskmanager.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.api.user.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Task API Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenA;
    private String tokenB;
    private String taskId;

    @BeforeAll
    void setup() throws Exception {
        tokenA = registerAndGetToken("tasktest-a-" + System.currentTimeMillis() + "@example.com");
        tokenB = registerAndGetToken("tasktest-b-" + System.currentTimeMillis() + "@example.com");
    }

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("SecurePass123!");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/tasks — should create a task")
    void shouldCreateTask() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Integration test task",
                                  "description": "Created by test",
                                  "status": "TODO",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Integration test task"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        taskId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/tasks — should reject missing title")
    void shouldRejectMissingTitle() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\": \"LOW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/tasks — should reject DELETED status on create")
    void shouldRejectDeletedStatusOnCreate() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Bad task", "status": "DELETED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/tasks — should list own tasks")
    void shouldListOwnTasks() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].title").value("Integration test task"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/tasks/{id} — should get own task")
    void shouldGetOwnTask() throws Exception {
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/tasks/{id} — other user gets 404 (ownership)")
    void otherUserGets404() throws Exception {
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/tasks/{id} — should update own task")
    void shouldUpdateOwnTask() throws Exception {
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(8)
    @DisplayName("PUT /api/tasks/{id} — other user cannot update (404)")
    void otherUserCannotUpdate() throws Exception {
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DONE"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/tasks/{id} — other user cannot delete (404)")
    void otherUserCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/tasks/{id} — should soft-delete own task")
    void shouldSoftDeleteOwnTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/tasks — should not return soft-deleted tasks")
    void shouldNotReturnDeletedTasks() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + taskId + "')]").doesNotExist());
    }

    @Test
    @Order(12)
    @DisplayName("POST /api/tasks — without auth returns 401")
    void noAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No auth task"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/tasks — filter by status")
    void shouldFilterByStatus() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Done task", "status": "DONE"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks?status=DONE")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("DONE"))));
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/tasks — filter by priority")
    void shouldFilterByPriority() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Low prio task", "priority": "LOW"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks?priority=LOW")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].priority", everyItem(is("LOW"))));
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/tasks/{id} — non-existent UUID returns 404")
    void nonExistentTaskReturns404() throws Exception {
        mockMvc.perform(get("/api/tasks/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
