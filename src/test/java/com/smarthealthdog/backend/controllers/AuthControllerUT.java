package com.smarthealthdog.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.LoginRequest;
import com.smarthealthdog.backend.dto.LoginResponse;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.dto.UserEmailVerifyRequest;
import com.smarthealthdog.backend.services.AuthService;
import com.smarthealthdog.backend.services.CustomUserDetailsService;
import com.smarthealthdog.backend.services.EmailService;
import com.smarthealthdog.backend.services.RefreshTokenService;
import com.smarthealthdog.backend.validation.ValidErrorCodeFinder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
class AuthControllerUT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mocking all dependencies of AuthController
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private ValidErrorCodeFinder validErrorCodeFinder;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final String BASE_URL = "/api/auth";
    private final String MOCK_EMAIL = "test@example.com";
    private final String MOCK_PASSWORD = "Password123!";
    private final Long MOCK_USER_ID = 1L;

    // --- Helper Methods ---

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
    
    // --- /login Tests ---

    @Test
    void authenticateUser_ShouldReturn200AndTokens_OnSuccess() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(MOCK_EMAIL, MOCK_PASSWORD);
        LoginResponse expectedResponse = new LoginResponse("mock.access.token", "mock.refresh.token");

        Authentication mockAuthentication = mock(Authentication.class);
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn(String.valueOf(MOCK_USER_ID));
        when(mockAuthentication.getPrincipal()).thenReturn(mockUserDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);

        // 2. Mock AuthService to return the tokens
        when(authService.generateTokens(MOCK_USER_ID))
                .thenReturn(expectedResponse);

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(expectedResponse.accessToken()))
                .andExpect(jsonPath("$.refreshToken").value(expectedResponse.refreshToken()));

        // VERIFY
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authService, times(1)).generateTokens(MOCK_USER_ID);
    }

    @Test
    void authenticateUser_ShouldReturn401_OnInvalidCredentials() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(MOCK_EMAIL, "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest)))
                // Spring Security's default exception handler often returns 401
                .andExpect(status().isUnauthorized());

        // VERIFY: AuthService should not be called
        verify(authService, never()).generateTokens(anyLong());
    }

    @Test
    void authenticateUser_ShouldReturn400_OnInvalidInput() throws Exception {
        // ARRANGE: Assuming LoginRequest has a validation that rejects a blank email.
        LoginRequest invalidRequest = new LoginRequest("", MOCK_PASSWORD);

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidRequest)))
                // @Valid failure typically results in a 400 Bad Request
                .andExpect(status().isBadRequest());

        // VERIFY: No services should be called
        verify(authenticationManager, never()).authenticate(any());
        verify(authService, never()).generateTokens(anyLong());
    }

    // --- /register Tests ---

    @Test
    void createUser_ShouldReturn201_OnSuccess() throws Exception {
        // ARRANGE
        UserCreateRequest request = new UserCreateRequest("John Doe", MOCK_EMAIL, MOCK_PASSWORD);
        User newUser = mock(User.class);

        when(authService.registerUser(request)).thenReturn(newUser);
        doNothing().when(emailService).sendEmailVerification(newUser);

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated()) // Expect 201 Created
                .andExpect(content().string("")); // Response body should be empty

        // VERIFY
        verify(authService, times(1)).registerUser(request);
        verify(emailService, times(1)).sendEmailVerification(newUser);
    }

    @Test
    void createUser_ShouldReturn400_OnInvalidRegistrationInput() throws Exception {
        // ARRANGE: Assuming validation rejects a name shorter than 3 characters
        UserCreateRequest invalidRequest = new UserCreateRequest("Jo", MOCK_EMAIL, MOCK_PASSWORD);

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidRequest)))
                .andExpect(status().isBadRequest()); // Expect 400 Bad Request

        // VERIFY: No services should be called
        verify(authService, never()).registerUser(any());
        verify(emailService, never()).sendEmailVerification(any());
    }

    // --- /register/verify-email Tests ---

    @Test
    void verifyEmail_ShouldReturn204_OnSuccess() throws Exception {
        // ARRANGE
        String token = "000000";
        UserEmailVerifyRequest request = new UserEmailVerifyRequest(MOCK_EMAIL, token);
        User userToActivate = mock(User.class);

        when(authService.verifyEmailToken(MOCK_EMAIL, token)).thenReturn(userToActivate);
        doNothing().when(authService).activateUser(userToActivate);

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/register/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isNoContent()) // Expect 204 No Content
                .andExpect(content().string("")); // Response body must be empty for 204

        // VERIFY
        verify(authService, times(1)).verifyEmailToken(MOCK_EMAIL, token);
        verify(authService, times(1)).activateUser(userToActivate);
    }

    @Test
    void verifyEmail_ShouldReturn400_OnInvalidTokenOrEmail() throws Exception {
        // ARRANGE
        String invalidToken = "expired-token";
        UserEmailVerifyRequest request = new UserEmailVerifyRequest(MOCK_EMAIL, invalidToken);

        // Assuming AuthService throws an exception (which should be handled by a global @ControllerAdvice
        // to return 400 or 404, but here we simulate an immediate failure for simplicity).
        // If your AuthService throws a custom exception, you must mock the exception and ensure
        // your setup maps it to a 4xx status. Here we rely on Spring's general behavior.
        when(authService.verifyEmailToken(MOCK_EMAIL, invalidToken))
                .thenThrow(new RuntimeException("Token invalid or expired"));

        // ACT & ASSERT
        mockMvc.perform(post(BASE_URL + "/register/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                // This status code depends heavily on your global exception handler.
                // 400 or 409 (Conflict) are common for business logic errors here.
                .andExpect(status().is(400)); 

        // VERIFY: activateUser should not be called
        verify(authService, never()).activateUser(any());
    }
}