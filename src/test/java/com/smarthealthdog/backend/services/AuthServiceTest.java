package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.validation.ErrorCode;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {
    @Autowired 
    private AuthService authService;

    @Autowired
    private UserService userService;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void registerUser_shouldReturnCreatedUser() {

        Role unverifiedUserRole = new Role();
        unverifiedUserRole.setName(RoleEnum.UNVERIFIED_USER);

        when(roleRepository.findByName(RoleEnum.UNVERIFIED_USER))
            .thenReturn(Optional.of(unverifiedUserRole));

        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest(
            "testuser",
            "test@example.com",
            "TestPassword123!"
        );

        // Act
        User createdUser = authService.registerUser(createRequest);

        assertTrue(createdUser.getRole().getName() == RoleEnum.UNVERIFIED_USER);
        assertTrue(createdUser.getEmail().equals("test@example.com"));
        assertTrue(createdUser.getNickname().equals("testuser"));

        // Compare the hashed password with the raw password
        assertTrue(createdUser.getPassword() != null);
        assertTrue(createdUser.getPassword().length() > 0);
        assertTrue(userService.checkUserPassword(createdUser, "TestPassword123!"));
    }

    @Test
    void verifyEmailToken_withInvalidEmail_shouldThrowException() {
        // Arrange
        String invalidEmail = "invalid@example.com";
        String token = "someInvalidToken";

        when(userRepository.findByEmail(invalidEmail)).thenReturn(Optional.empty());

        // Use assertThrows to check if the method throws the correct exception
        InvalidRequestDataException exception = assertThrows(
            InvalidRequestDataException.class,
            // Lambda function that calls the method being tested
            () -> authService.verifyEmailToken(invalidEmail, token),
            "Expected verifyEmailToken to throw InvalidRequestDataException for invalid email"
        );

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    void verifyEmailToken_withExceedingFailCount_shouldThrowException() {
        // Arrange
        String email = "test@example.com";
        String token = "someValidToken";

        User user = new User();
        user.setEmailVerificationFailCount(5); // Exceeding the limit

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Use assertThrows to check if the method throws the correct exception
        InvalidRequestDataException exception = assertThrows(
            InvalidRequestDataException.class,
            // Lambda function that calls the method being tested
            () -> authService.verifyEmailToken(email, token),
            "Expected verifyEmailToken to throw InvalidRequestDataException for exceeding fail count"
        );

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    void verifyEmailToken_withNonUnverifiedUserRole_shouldThrowException() {
        // Arrange
        String email = "test@example.com";
        String token = "someValidToken";

        User user = new User();
        Role role = new Role();
        role.setName(RoleEnum.USER); // Not UNVERIFIED_USER role

        user.setEmailVerificationFailCount(0);
        user.setRole(role); // Not UNVERIFIED_USER role

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        // Use assertThrows to check if the method throws the correct exception

        InvalidRequestDataException exception = assertThrows(
            InvalidRequestDataException.class,
            // Lambda function that calls the method being tested
            () -> authService.verifyEmailToken(email, token),
            "Expected verifyEmailToken to throw InvalidRequestDataException for non-UNVERIFIED_USER role"
        );

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    void verifyEmailToken_withExpiredToken_shouldThrowException() {
        // Arrange
        String email = "test@example.com";
        String token = "someExpiredToken";
        User user = new User();
        Role role = new Role();
        role.setName(RoleEnum.UNVERIFIED_USER); // UNVERIFIED_USER role
        user.setRole(role);
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(java.time.Instant.now().minusSeconds(3600)); // Expired 1 hour ago

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Use assertThrows to check if the method throws the correct exception
        InvalidRequestDataException exception = assertThrows(
            InvalidRequestDataException.class,
            // Lambda function that calls the method being tested
            () -> authService.verifyEmailToken(email, token),
            "Expected verifyEmailToken to throw InvalidRequestDataException for expired token"
        );

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    void verifyEmailToken_withMismatchedToken_shouldThrowException() {
        // Arrange
        String email = "test@example.com";
        String token = "someMismatchedToken";

        User user = new User();
        Role role = new Role();
        role.setName(RoleEnum.UNVERIFIED_USER); // UNVERIFIED_USER role
        user.setRole(role);

        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(java.time.Instant.now().plusSeconds(3600)); // Valid for 1 more hour
        user.setEmailVerificationToken("actualTokenValue"); // Actual token value

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.incrementEmailVerificationFailCount(user.getId())).thenAnswer(invocation -> {
            user.setEmailVerificationFailCount(user.getEmailVerificationFailCount() + 1);
            return user.getEmailVerificationFailCount();
        });

        // Use assertThrows to check if the method throws the correct exception
        InvalidRequestDataException exception = assertThrows(
            InvalidRequestDataException.class,
            // Lambda function that calls the method being tested
            () -> authService.verifyEmailToken(email, token),
            "Expected verifyEmailToken to throw InvalidRequestDataException for mismatched token"
        );

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL_VERIFICATION);
        assertTrue(user.getEmailVerificationFailCount() == 1, "The email verification fail count should be incremented.");
    }

    @Test
    void verifyEmailToken_withValidToken_shouldReturnUser() {
        // Arrange
        String email = "test@example.com";
        String token = "someValidToken";

        User user = new User();
        Role role = new Role();
        role.setName(RoleEnum.UNVERIFIED_USER); // UNVERIFIED_USER role
        user.setRole(role);
        user.setEmailVerificationFailCount(0);
        user.setEmailVerificationExpiry(java.time.Instant.now().plusSeconds(3600)); // Valid for 1 more hour
        user.setEmailVerificationToken(token); // Matching token

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.incrementEmailVerificationFailCount(user.getId())).thenReturn(1);
        // Act

        User returnedUser = authService.verifyEmailToken(email, token);
        // Assert

        assertTrue(returnedUser == user, "The returned user should match the expected user.");
        verify(userRepository).findByEmail(email);
    }

    @Test
    void activateUser_withNonUnverifiedUserRole_shouldThrowException() {
        // Arrange
        User user = new User();
        Role role = new Role();
        role.setName(RoleEnum.USER); // Not UNVERIFIED_USER role
        user.setRole(role);

        // Use assertThrows to check if the method throws the correct exception
        InvalidRequestDataException exception = assertThrows(
            InvalidRequestDataException.class,
            // Lambda function that calls the method being tested
            () -> authService.activateUser(user),
            "Expected activateUser to throw InvalidRequestDataException for non-UNVERIFIED_USER role"
        );

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    @Test
    void activateUser_withUnverifiedUserRole_shouldActivateUser() {
        // Arrange
        User user = new User();
        Role unverifiedRole = new Role();
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);
        user.setRole(unverifiedRole);

        Role verifiedRole = new Role();
        verifiedRole.setName(RoleEnum.USER);

        when(roleRepository.findByName(RoleEnum.USER))
            .thenReturn(Optional.of(verifiedRole));

        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.resetEmailVerificationFailCount(user.getId())).thenAnswer(invocation -> {
            user.setEmailVerificationFailCount(0);
            return user.getEmailVerificationFailCount();
        });

        // Act
        authService.activateUser(user);

        // Assert
        assertTrue(user.getRole().getName() == RoleEnum.USER, "The user's role should be changed to USER.");
        assertTrue(user.getEmailVerificationToken() == null, "The email verification token should be expired (set to null).");
        assertTrue(user.getEmailVerificationFailCount() == 0, "The email verification fail count should be reset to 0.");

        verify(userRepository, times(2)).save(user);
    }
}
