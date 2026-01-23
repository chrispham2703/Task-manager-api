package com.taskmanager.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.api.user.LoginRequest;
import com.taskmanager.api.user.RefreshTokenRequest;
import com.taskmanager.api.user.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authentication Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should register new user and return tokens")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser" + System.currentTimeMillis() + "@example.com");
            request.setPassword("SecurePass123!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.email").value(request.getEmail().toLowerCase()))
                    .andExpect(jsonPath("$.user.id").exists())
                    .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("should reject invalid email")
        void shouldRejectInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("invalid-email");
            request.setPassword("SecurePass123!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject missing password")
        void shouldRejectMissingPassword() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            // First register a user
            String email = "logintest" + System.currentTimeMillis() + "@example.com";
            String password = "SecurePass123!";

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(email);
            registerRequest.setPassword(password);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // Then login
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword(password);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        @DisplayName("should reject wrong password")
        void shouldRejectWrongPassword() throws Exception {
            // First register a user
            String email = "wrongpass" + System.currentTimeMillis() + "@example.com";

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(email);
            registerRequest.setPassword("CorrectPass123!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated());

            // Try login with wrong password
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword("WrongPass123!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Protected Endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("should reject access without token")
        void shouldRejectWithoutToken() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject access with invalid token")
        void shouldRejectInvalidToken() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should allow access with valid token")
        void shouldAllowWithValidToken() throws Exception {
            // Register and get token
            String email = "protected" + System.currentTimeMillis() + "@example.com";
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(email);
            registerRequest.setPassword("SecurePass123!");

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            String accessToken = objectMapper.readTree(response).get("accessToken").asText();

            // Access protected endpoint
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email.toLowerCase()));
        }
    }
}
