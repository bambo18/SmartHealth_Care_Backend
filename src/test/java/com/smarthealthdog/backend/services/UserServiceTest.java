package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.validation.ErrorCode;


@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {
    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @Test
    public void createUser_withInvalidNickname_shouldThrowException() {
        // Arrange
        String invalidNickname = "ab"; // Too short, assuming min length is 3
        String email = "test@example.com";
        String password = "ValidPass123!";

        // Act & Assert
        InvalidRequestDataException exception = assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(invalidNickname, email, password);
        });

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_NICKNAME);
    }

    @Test
    public void createUser_withExistingEmail_shouldThrowException() {
        // Arrange
        String nickname = "validNickname";
        String existingEmail = "test@example.com";
        String password = "ValidPass123!";

        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);
        // Act & Assert
        InvalidRequestDataException exception = assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname, existingEmail, password);
        });

        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void createUser_withInvalidPassword_shouldThrowException() {
        // Arrange
        String nickname = "validNickname";
        String email = "test@example.com";
        String password = "invalid"; // Invalid password, assuming min length is 8

        when(userRepository.existsByEmail(email)).thenReturn(false);
        // Act & Assert
        InvalidRequestDataException exception = assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname, email, password);
        });
        // Assert that the exception contains the expected error code
        assertTrue(exception.getErrorCode() == ErrorCode.INVALID_PASSWORD);
    }

    @Test
    public void createUser_withValidData_shouldCreateUser() {
        // Arrange
        String nickname = "validNickname";
        String email = "test@example.com";
        String password = "ValidPass123!";

        Role unverifiedRole = new Role();
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName(RoleEnum.UNVERIFIED_USER)).thenReturn(Optional.of(unverifiedRole));

        // Act
        User user = userService.createUser(nickname, email, password);

        // Assert
        assertNotNull(user);
        assertEquals(nickname, user.getNickname());
        assertEquals(email, user.getEmail());
        assertTrue(user.getRole().getName() == RoleEnum.UNVERIFIED_USER);
        // Password should be hashed, so it should not match the raw password

        assertTrue(user.getPassword() != null);
        assertTrue(user.getPassword().length() > 0);
        assertTrue(userService.checkUserPassword(user, password));
    }
}