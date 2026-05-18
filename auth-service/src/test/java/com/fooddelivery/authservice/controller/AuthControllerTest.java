package com.fooddelivery.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.authservice.authservice.AuthService;
import com.fooddelivery.authservice.dto.LoginRequest;
import com.fooddelivery.authservice.dto.LoginResponse;
import com.fooddelivery.authservice.dto.RegisterRequest;
import com.fooddelivery.authservice.config.JwtAuthFilter;
import com.fooddelivery.authservice.config.SecurityConfig;
import com.fooddelivery.authservice.exception.GlobalExceptionHandler;
import com.fooddelivery.authservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    // --- login ---

    @Test
    void loginShouldReturn200AndTokenWhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "testpassword");
        LoginResponse response = LoginResponse.of("mocked-jwt-token", 86400000);

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void loginShouldReturn401WhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(authService.login(request))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void loginShouldReturn400WhenUsernameIsBlank() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"testpassword\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturn400WhenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- register ---

    @Test
    void registerShouldReturn201WhenRequestIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "password123");

        doNothing().when(authService).register(request);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void registerShouldReturn400WhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldReturn409WhenUsernameAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("existinguser", "password123");

        doThrow(new IllegalArgumentException("Username already exists"))
                .when(authService).register(request);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    // --- promote ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void promoteShouldReturn200WhenCalledByAdmin() throws Exception {
        doNothing().when(authService).promoteToAdmin("targetuser");

        mockMvc.perform(post("/auth/admin/promote/targetuser"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void promoteShouldReturn403WhenCalledByRegularUser() throws Exception {
        mockMvc.perform(post("/auth/admin/promote/targetuser"))
                .andExpect(status().isForbidden());
    }
}