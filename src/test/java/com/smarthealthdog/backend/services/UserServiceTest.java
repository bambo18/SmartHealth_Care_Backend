package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.smarthealthdog.backend.domain.Permission;
import com.smarthealthdog.backend.domain.PermissionEnum;
import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.UserProfile;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.repositories.PermissionRepository;
import com.smarthealthdog.backend.repositories.RoleRepository;
import com.smarthealthdog.backend.repositories.UserRepository;


@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeAll
    void setup() {
        // iterate over Enum values and create permissions
        // // --- General User Permissions (User & Profile) ---
        // CAN_VIEW_OWN_PROFILE("can_view_own_profile", "자신의 프로필 보기"),
        Permission viewOwnProfilePermission = new Permission();
        viewOwnProfilePermission.setName(PermissionEnum.CAN_VIEW_OWN_PROFILE);
        viewOwnProfilePermission.setDescription("자신의 프로필 보기");
        permissionRepository.save(viewOwnProfilePermission);

        Role role = new Role();
        role.setName(RoleEnum.USER);
        role.setDescription("Standard user role");
        role.setPermissions(new java.util.HashSet<>());
        roleRepository.save(role);

        role.getPermissions().add(viewOwnProfilePermission);
        roleRepository.save(role);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @AfterAll
    void tearDown() {
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
    }

    @Test
    public void createUser_ShouldThrowInvalidRequestDataException_WhenNicknameIsInvalid() {
        String invalidNickname = "ab"; // 너무 짧은 닉네임
        String email = "testuser@example.com";
        String password = "ValidPass1!";

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(invalidNickname, email, password);
        });

        String longNickname = "a".repeat(129); // 너무 긴 닉네임
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(longNickname, email, password);
        });
    }

    @Test
    public void createUser_ShouldThrowInvalidRequestDataException_WhenPasswordIsInvalid() {
        String nickname = "validNickname";
        String email = "testuser@example.com";
        String invalidPassword = "short"; // 너무 짧은 비밀번호

        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname, email, invalidPassword);
        });

        String noUpperCasePassword = "lowercase1!"; // 대문자 없음
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname, email, noUpperCasePassword);
        });

        String noSpecialCharPassword = "NoSpecialChar1"; // 특수문자 없음
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname, email, noSpecialCharPassword);
        });

        String noNumberPassword = "NoNumber!"; // 숫자 없음
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname, email, noNumberPassword);
        });
    }

    @Test
    public void createUser_ShouldThrowInvalidRequestDataException_WhenEmailAlreadyExists() {
        String nickname1 = "userOne";
        String email = "testuser@example.com";
        String password = "ValidPass1!";

        userService.createUser(nickname1, email, password);

        boolean userExists = userRepository.existsByEmail(email);
        assertTrue(userExists);

        String nickname2 = "userTwo";
        // ACT & ASSERT
        assertThrows(InvalidRequestDataException.class, () -> {
            userService.createUser(nickname2, email, password);
        });
    }

    @Test
    public void createUser_ShouldCreateUserSuccessfully_WhenInputIsValid() {
        String nickname = "validNickname";
        String email = "testuser@example.com";
        String password = "ValidPass1!";

        userService.createUser(nickname, email, password);

        User user = userRepository.findByEmail(email).orElse(null);
        assertNotNull(user);

        assertTrue(user.getNickname().equals(nickname));
        assertTrue(user.getEmail().equals(email));
        assertTrue(user.getPassword() != null);
        assertTrue(user.getPassword() != password); // 비밀번호는 해시화되어 저장되므로 원본과 다름
    }

    @Test
    public void getUserProfileById_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        Long nonExistingUserId = 999L;

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserProfileById(nonExistingUserId);
        });
    }
    
    @Test
    public void getUserProfileById_ShouldReturnUserProfile_WhenUserExists() {
        User user = userService.createUser("asdfasdf", "testuser@example.com", "ValidPass1!");
        
        boolean userExists = userRepository.findById(user.getId()).isPresent();
        assertTrue(userExists);

        UserProfile userProfile = userService.getUserProfileById(user.getId());
        assertNotNull(userProfile);
        assertEquals(user.getId(), userProfile.id());
        assertEquals(user.getEmail(), userProfile.email());
        assertEquals(user.getNickname(), userProfile.nickname());
        assertEquals(user.getProfilePic(), userProfile.profileImgUrl());
    }
}