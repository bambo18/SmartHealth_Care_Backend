package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.validation.NicknameValidator;
import com.smarthealthdog.backend.validation.PasswordValidator;

@ExtendWith(MockitoExtension.class) 
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private NicknameValidator nicknameValidator;

    @InjectMocks
    private UserService userService;

    private Role unverifiedRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Initialization if needed
        // Create roles
        unverifiedRole = new Role();
        unverifiedRole.setId((short)1);
        unverifiedRole.setName(RoleEnum.UNVERIFIED_USER);

        userRole = new Role();
        userRole.setId((short)2);
        userRole.setName(RoleEnum.USER);
    }

    @Test
    public void testCreateUser_Success() {
        // ARRANGE: Set up the mock behavior for the success path
        // 1. **FIX:** Tell the nicknameValidator to return TRUE
        when(nicknameValidator.isValid(anyString())).thenReturn(true); 

        // 2. Tell the userRepository that no user exists (so creation can proceed)
        when(userRepository.existsByEmail(anyString())).thenReturn(false); 
        
        // 3. Tell the passwordValidator to return TRUE
        when(passwordValidator.isValid(anyString())).thenReturn(true); 

        // 4. Mock the role service to return a non-null Role object
        when(roleService.getUnverifiedUserRole()).thenReturn(new Role()); 
        
        // 5. Mock the userRepository.save() call (to return the created user)
        // This is often good practice to ensure the service proceeds to the end.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userArg = invocation.getArgument(0);
            userArg.setId(1L); // Simulate DB assigning an ID
            userArg.setCreatedAt(java.time.Instant.now());
            userArg.setUpdatedAt(java.time.Instant.now());
            return userArg;
        });

        // Implement test logic here
        User user = userService.createUser(
            "nickname", 
            "test@email.com",
            "ValidPassword123!"
        );

        assertTrue(user != null);
        assertTrue(user.getNickname().equals("nickname"));
        assertTrue(user.getEmail().equals("test@email.com"));
        assertTrue(user.getCreatedAt() != null);
        assertTrue(user.getUpdatedAt() != null);
        assertTrue(user.getRole() != null);
        assertTrue(user.getLoginAttempt() == 0);
        assertTrue(user.getId() != null); // ID should not be null after saving to DB
        assertTrue(user.getProfilePic() == null);
        assertTrue(user.getPassword() == null); // Password should not be exposed
        assertTrue(user.getLoginAttempt() == 0);
        assertTrue(user.getLoginAttemptStartedAt() == null);
        assertTrue(user.getPasswordResetToken() == null);
        assertTrue(user.getPasswordResetTokenExpiry() == null);
        assertTrue(user.getPasswordResetRequestedAt() == null);
        assertTrue(user.getPasswordResetTokenVerifyFailCount() == 0);
        assertTrue(user.getEmailVerificationToken() == null);
        assertTrue(user.getEmailVerificationRequestedAt() == null);
        assertTrue(user.getEmailVerificationExpiry() == null);
        assertTrue(user.getEmailVerificationFailCount() == 0);
    }

    @Test
    public void testChangeRoleToVerifiedUser_Success() {
        User user = new User();
        user.setId(1L);
        user.setRole(unverifiedRole);

        // Mock the roleService to return the USER role
        when(roleService.getUserRole()).thenReturn(userRole);

        // ACT
        userService.changeRoleToVerifiedUser(user);

        // ASSERT: Verify the user's role has been changed to USER
        assertTrue(user.getRole().getName().equals(RoleEnum.USER));
    }

    @Test
    public void testExpireEmailVerificationToken_Success() {
        User user = new User();
        user.setId(1L);
        user.setEmailVerificationExpiry(null); // Initially null

        // ACT
        userService.expireEmailVerificationToken(user);

        // ASSERT: Verify the email verification expiry has been set to now
        assertTrue(user.getEmailVerificationExpiry() != null);
    }

    @Test
    public void testResetEmailVerificationFailCount_Success() {
        User user = new User();
        user.setId(1L);

        // ACT
        userService.resetEmailVerificationFailCount(user);

        // ASSERT: Since this method does not return anything, we verify that the repository method was called
        // This is done via Mockito's verify in the actual test framework, but here we just ensure no exceptions occur.
        verify(userRepository).resetEmailVerificationFailCount(user.getId());
    }
}