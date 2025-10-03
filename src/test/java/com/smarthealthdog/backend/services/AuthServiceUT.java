package com.smarthealthdog.backend.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.LoginResponse;
import com.smarthealthdog.backend.dto.UserCreateRequest;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceUT {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    // --- Setup for verifyEmailToken tests ---

    private User createVerifiableUser(String email, String token, Instant expiry) {
        Role role = new Role();
        role.setName(RoleEnum.UNVERIFIED_USER);
        role.setDescription("Role for unverified users");

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiry(expiry);
        user.setEmailVerificationFailCount((short) 0);
        user.setRole(role);
        return user;
    }

    // --- Setup for verifiedUser tests ---
    private User createVerifiedUser() {
        Role verifiedRole = new Role();
        verifiedRole.setName(RoleEnum.USER);
        verifiedRole.setDescription("Role for verified users");

        User user = new User();
        user.setId(2L);
        user.setEmail("verified_already@example.com");
        user.setRole(verifiedRole);
        // Ensure no verification data is set
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        user.setEmailVerificationFailCount((short) 0);
        return user;
    }

    @Test
    void registerUser_shouldCallUserServiceCreateUser_andReturnUser() {
        String nickname = "test_nick";
        String email = "test@example.com";
        String password = "StrongPassword123!";
        String passwordBCryptHash = "$2a$12$MFXz7l1CM1PwjILa/FvfQOImgCIYOwp/EtyHq0RaPg4fkgu.CxIhq"; // Example hash
        
        // Use a record or class that matches your method signature
        UserCreateRequest mockRequest = new UserCreateRequest(nickname, email, password);

        // Create a fake User object that the mock UserService will return
        User expectedUser = new User(); 
        expectedUser.setId(1L);
        expectedUser.setNickname(nickname);
        expectedUser.setEmail(email);
        expectedUser.setPassword(passwordBCryptHash);

        // Program the mock: Tell the UserService what to return when it's called
        when(userService.createUser(nickname, email, password))
            .thenReturn(expectedUser);

        // ACT
        User actualUser = authService.registerUser(mockRequest);

        // 3. VERIFY: Assert that the AuthService correctly delegated the call 
        // to the UserService with the exact arguments from the request.
        verify(userService).createUser(
            nickname, 
            email, 
            password
        );

        // Assert that AuthService returned the User object received from UserService
        assertEquals(expectedUser, actualUser);
    }

    @Test
    void verifyEmailToken_shouldReturnUser_whenTokenIsValid() {
        // ARRANGE
        String email = "valid@example.com";
        String token = "valid_token";
        // Set expiry to a time in the future (e.g., 5 minutes from now)
        Instant futureExpiry = Instant.now().plusSeconds(300);

        User expectedUser = createVerifiableUser(email, token, futureExpiry);

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(expectedUser));

        // ACT
        User actualUser = authService.verifyEmailToken(email, token);

        // ASSERT
        // Verify getUserByEmail was called
        verify(userService).getUserByEmail(email);

        // Ensure incrementEmailVerificationFailCount was NOT called
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(expectedUser);

        // Assert the correct user was returned
        assertEquals(expectedUser, actualUser);
    }

    @Test
    void verifyEmailToken_shouldThrowException_whenUserNotFound() {
        // ARRANGE
        String email = "notfound@example.com";
        String token = "any_token";

        when(userService.getUserByEmail(email)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, token);
        });


        // Verify the correct exception details
        // assertEquals(ErrorCode.INVALID_EMAIL_VERIFICATION, exception.getErrorCode()); // Assuming getErrorCode exists
        verify(userService).getUserByEmail(email);
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(org.mockito.Mockito.any());

    }

    @Test
    void verifyEmailToken_shouldThrowException_whenFailCountIsFiveOrMore() {
        // ARRANGE
        String email = "failcount@example.com";
        String token = "valid_token";
        Instant futureExpiry = Instant.now().plusSeconds(300);

        User user = createVerifiableUser(email, token, futureExpiry);
        user.setEmailVerificationFailCount((short) 5); // Fail count is 5

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(user));

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, token);
        });
        
        verify(userService).getUserByEmail(email);
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(user);
    }

    @Test
    void verifyEmailToken_shouldThrowException_whenUserIsAlreadyVerified() {
        // ARRANGE
        String email = "verified@example.com";
        String token = "valid_token";
        Instant futureExpiry = Instant.now().plusSeconds(300);

        User user = createVerifiableUser(email, token, futureExpiry);
        Role verifiedRole = new Role();
        verifiedRole.setName(RoleEnum.USER);
        // Change role to a verified role
        user.setRole(verifiedRole);

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(user));

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, token);
        });

        verify(userService).getUserByEmail(email);
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(user);
    }

    @Test
    void verifyEmailToken_shouldThrowException_whenExpiryTimeIsNull() {
        // ARRANGE
        String email = "noexpiry@example.com";
        String token = "valid_token";
        
        User user = createVerifiableUser(email, token, null); // Expiry is null

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(user));

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, token);
        });

        verify(userService).getUserByEmail(email);
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(user);
    }

    @Test
    void verifyEmailToken_shouldThrowException_whenTokenIsExpired() {
        // ARRANGE
        String email = "expired@example.com";
        String token = "valid_token";
        // Set expiry to a time in the past
        Instant pastExpiry = Instant.now().minusSeconds(300);

        User user = createVerifiableUser(email, token, pastExpiry);

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(user));

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, token);
        });

        verify(userService).getUserByEmail(email);
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(user);
    }

    @Test
    void verifyEmailToken_shouldIncrementFailCountAndThrowException_whenTokenMismatches() {
        // ARRANGE
        String email = "mismatch@example.com";
        String correctToken = "correct_token";
        String wrongToken = "wrong_token";
        Instant futureExpiry = Instant.now().plusSeconds(300);

        User user = createVerifiableUser(email, correctToken, futureExpiry);
        // Set fail count to 4 (so it doesn't fail the count check but is ready for increment)
        user.setEmailVerificationFailCount((short) 4);

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(user));

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, wrongToken); // ACT with the WRONG token
        });

        // VERIFY
        verify(userService).getUserByEmail(email);
        // Must verify that the fail count increment was called when the token mismatches
        verify(userService).incrementEmailVerificationFailCount(user);
    }

    @Test
    void verifyEmailToken_shouldThrowException_whenUserNotFound_case2() {
        // ARRANGE
        String email = "notfound@example.com";

        String wrongToken = "wrong_token";
        when(userService.getUserByEmail(email)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.verifyEmailToken(email, wrongToken);
        });

        verify(userService).getUserByEmail(email);
        verify(userService, org.mockito.Mockito.never())
            .incrementEmailVerificationFailCount(org.mockito.Mockito.any());
        // verifyNoInteractions(userService); // Ensure no other interactions occurred
    }

    @Test
    void activateUser_shouldThrowException_whenUserIsAlreadyVerified() {
        // ARRANGE
        User verifiedUser = createVerifiedUser();

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            authService.activateUser(verifiedUser);
        });

        // VERIFY
        verify(userService, org.mockito.Mockito.never())
            .changeRoleToVerifiedUser(verifiedUser);
        verify(userService, org.mockito.Mockito.never())
            .expireEmailVerificationToken(verifiedUser);
        verify(userService, org.mockito.Mockito.never())
            .resetEmailVerificationFailCount(verifiedUser);
    }

    @Test
    void activateUser_shouldCallUserServiceMethods_whenUserIsUnverified() {
        // ARRANGE
        String email = "unverified@example.com";
        User unverifiedUser = createVerifiableUser(
            email, 
            "valid_token", 
            Instant.now().plusSeconds(300)
        );

        // ACT
        authService.activateUser(unverifiedUser);

        // VERIFY
        verify(userService).changeRoleToVerifiedUser(unverifiedUser);
        verify(userService).expireEmailVerificationToken(unverifiedUser);
        verify(userService).resetEmailVerificationFailCount(unverifiedUser);
    }

    @Test
    void generateTokens_shouldThrowException_whenUserNotFound() {
        // ARRANGE
        Long userId = 999L; // Non-existent user ID

        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.generateTokens(userId);
        });

        verify(userService).getUserById(userId);
    }

    @Test
    void generateTokens_shouldReturnLoginResponse_whenUserExists() {
        // ARRANGE
        User mockUser = mock(User.class);
        when(userService.getUserById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(refreshTokenService.generateRefreshToken(mockUser)).thenReturn("mockRefreshToken");
        when(refreshTokenService.generateAccessToken("mockRefreshToken")).thenReturn("mockAccessToken");

        // ACT
        LoginResponse response = authService.generateTokens(mockUser.getId());

        // ASSERT
        assertEquals("mockAccessToken", response.accessToken());
        assertEquals("mockRefreshToken", response.refreshToken());
    }
}
