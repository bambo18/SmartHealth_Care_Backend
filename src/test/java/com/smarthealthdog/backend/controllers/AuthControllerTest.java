package com.smarthealthdog.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.dto.UserEmailVerifyRequest;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.services.EmailService;

@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureMockMvc // Provides MockMvc instance
public class AuthControllerTest {
    // Test methods would go here
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @BeforeAll
    void setupAll() {
        doNothing().when(emailService).sendEmailVerification(org.mockito.ArgumentMatchers.any(User.class));

        // Runs once before all tests
        Role unverifiedRole = new Role();
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);
        unverifiedRole.setDescription("Role for unverified users");

        roleRepository.save(unverifiedRole);

        Role userRole = new Role();
        userRole.setName(RoleEnum.USER);
        userRole.setDescription("Role for regular users");
        roleRepository.save(userRole);

        roleRepository.flush();
    }

    @AfterEach
    void tearDown() {
        // Runs after each test
        userRepository.deleteAll();
    }

    @AfterAll
    void tearDownAll() {
        // Runs once after all tests
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void createUser_ShouldReturn400BadRequest_WhenRequestPasswordIsInvalid() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "testuser",
            "testuser@example.com",
            "password123"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturn400BadRequest_WhenRequestEmailIsInvalid() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "testuser",
            "invalid-email",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturn400BadRequest_WhenRequestNicknameIsBlank() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "",
            "testuser@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturn400BadRequest_WhenRequestNicknameIsTooShort() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "aa", // Assuming min length is 3
            "testuser@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturn400BadRequest_WhenRequestNicknameIsTooLong() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "a".repeat(129), // Assuming max length is 128
            "testuser@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturn400BadRequest_WhenRequestEmailIsBlank() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "testuser",
            "",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_ShouldReturn201Created_WhenRequestIsValid() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "testuser",
            "testuser@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isCreated());
    }

    @Test
    void verifyEmail_ShouldReturn204NoContent_WhenRequestIsValid() throws Exception {
        // First, create a user to get a valid email verification token
        UserCreateRequest createRequest = new UserCreateRequest(
            "verifyuser",
            "verifyuser@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        // 사용자 조회
        Optional<User> userOpt = userRepository.findByEmail("verifyuser@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();

        assertTrue(user.getRole().getName().equals(RoleEnum.UNVERIFIED_USER));

        // 이메일 서비스를 모킹했기 때문에 토큰이 설정되지 않음
        // 수동으로 토큰과 만료 시간을 설정
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(60 * 15)); // 15 minutes from now
        userRepository.save(user);
        userRepository.flush();

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "verifyuser@example.com",
            "000000"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());

        // Verify that the user's role has been updated to USER
        userOpt = userRepository.findByEmail("verifyuser@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        assertTrue(user.getRole().getName().equals(RoleEnum.USER));
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenRequestEmailIsInvalid() throws Exception {
        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "invalid-email",
            "000000"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenRequestEmailIsBlank() throws Exception {
        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "",
            "000000"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenRequestTokenIsBlank() throws Exception {
        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "differentUser@example.com",
            ""
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenTokenLengthIsNotSix() throws Exception {
        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "differentUser@example.com",
            "12345"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenUserIsNotFound() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
            "nonexistentuser",
            "nonexistent@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "differentUser@example.com",
            "000000"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenEmailVerificationFailCountExceedsLimit() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
            "nonexistentuser",
            "nonexistent@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        // 이메일 서비스를 모킹했기 때문에 토큰이 설정되지 않음
        // 수동으로 토큰과 만료 시간을 설정
        Optional<User> userOpt = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        user.setEmailVerificationFailCount(5); // Exceed the limit
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(60 * 15)); //

        userRepository.save(user);
        userRepository.flush();

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "nonexistent@example.com",
            "000000"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenEmailVerificationTokenIsExpired() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
            "nonexistentuser",
            "nonexistent@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        // 이메일 서비스를 모킹했기 때문에 토큰이 설정되지 않음
        // 수동으로 토큰과 만료 시간을 설정
        Optional<User> userOpt = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationExpiry(Instant.now().minusSeconds(60 * 15)); // 15 minutes ago
        userRepository.save(user);
        userRepository.flush();

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "nonexistent@example.com",
            "000000"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenUserIsAlreadyVerified() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
            "nonexistentuser",
            "nonexistent@example.com",
            "Password123!"
        );
        
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        // 이메일 서비스를 모킹했기 때문에 토큰이 설정되지 않음
        // 수동으로 토큰과 만료 시간을 설정
        Optional<User> userOpt = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(60 * 15)); // 15 minutes from now
        user.setRole(roleRepository.findByName(RoleEnum.USER).get()); // Set role to USER (already verified)
        userRepository.save(user);
        userRepository.flush();

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "nonexistent@example.com",
            "000000"
        );
        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_ShouldReturn400BadRequest_WhenRequestTokenIsIncorrect() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
            "nonexistentuser",
            "nonexistent@example.com",
            "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        // 이메일 서비스를 모킹했기 때문에 토큰이 설정되지 않음
        // 수동으로 토큰과 만료 시간을 설정
        Optional<User> userOpt = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(60 * 15)); // 15 minutes from now
        userRepository.save(user);
        userRepository.flush();

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "nonexistent@example.com",
            "000001"
        );

        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());

        // Verify that the email verification fail count has been incremented
        userOpt = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        assertTrue(user.getEmailVerificationFailCount() == 1);

        // 한 번 더 틀리기 - 2회
        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isBadRequest());

        userOpt = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        assertTrue(user.getEmailVerificationFailCount() == 2);
    }

    @Test
    void activateUser_ShouldThrowException_WhenUserIsAlreadyActive() {
        // This test is effectively covered in the verifyEmail_ShouldReturn400BadRequest_WhenUserIsAlreadyVerified test
        // because the activateUser method is called within the email verification process.
        // Therefore, we can leave this test empty.
    }

    @Test
    void activateUser_ShouldActivateUser_WhenUserIsUnverified() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
            "activatetestuser",
            "activatetestuser@example.com",
            "Password123!"
        );
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(createRequest)))
            .andExpect(status().isCreated());

        Optional<User> userOpt = userRepository.findByEmail("activatetestuser@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationToken("000000");
        user.setEmailVerificationExpiry(Instant.now().plusSeconds(60 * 15)); // 15 minutes from now
        userRepository.save(user);
        userRepository.flush();

        UserEmailVerifyRequest request = new UserEmailVerifyRequest(
            "activatetestuser@example.com",
            "000000"
        );
        mockMvc.perform(post("/api/auth/register/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(request)))
            .andExpect(status().isNoContent());

        userOpt = userRepository.findByEmail("activatetestuser@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        assertTrue(user.getRole().getName().equals(RoleEnum.USER));
        Instant now = Instant.now();
        assertTrue(now.isAfter(user.getEmailVerificationExpiry()));
        assertTrue(user.getEmailVerificationFailCount() == 0);
    }
}
