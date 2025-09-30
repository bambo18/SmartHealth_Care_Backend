package com.smarthealthdog.backend.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role testRole;

    // 테스트 실행 전에 필요한 데이터 설정
    @BeforeEach
    void setUp() {
        // 1. Create a new Role instance
        Role role = new Role(); // Use your Role constructor/setter
        role.setName(RoleEnum.UNVERIFIED_USER);
        role.setDescription("Role for unverified users");

        // 2. Persist the Role to the database
        this.testRole = roleRepository.save(role);
        
        // Ensure the data is immediately available in the context (optional, but good for safety)
        roleRepository.flush(); 
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        // Arrange: Insert test data using the repository itself
        User user = new User();
        user.setNickname("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        user.setRole(this.testRole); // Use the persisted role

        User savedUser = userRepository.save(user);

        // Act
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    void findByNickname_ShouldReturnUser_WhenNicknameExists() {
        // Arrange: Insert test data using the repository itself
        User user = new User();
        user.setNickname("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        user.setRole(this.testRole); // Use the persisted role

        User savedUser = userRepository.save(user);

        // Act
        Optional<User> foundUser = userRepository.findByNickname("testuser");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getNickname(), foundUser.get().getNickname());
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        // Arrange: Insert test data using the repository itself
        User user = new User();
        user.setNickname("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        user.setRole(this.testRole); // Use the persisted role

        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByNickname_ShouldReturnTrue_WhenNicknameExists() {
        // Arrange: Insert test data using the repository itself
        User user = new User();
        user.setNickname("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        user.setRole(this.testRole); // Use the persisted role

        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByNickname("testuser");

        // Assert
        assertTrue(exists);
    }
    @Test
    void existsById_ShouldReturnTrue_WhenIdExists() {
        // Arrange: Insert test data using the repository itself
        User user = new User();
        user.setNickname("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        user.setRole(this.testRole); // Use the persisted role

        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsById(user.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void incrementEmailVerificationFailCount_ShouldIncrementCount() {
        // Arrange: Insert test data using the repository itself
        User user = new User();
        user.setNickname("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        // Initial fail count is 0 of Short type
        user.setRole(this.testRole); // Use the persisted role
        User savedUser = userRepository.save(user);

        assertEquals((short) 0, savedUser.getEmailVerificationFailCount());

        // Act
        int newCount = userRepository.incrementEmailVerificationFailCount(savedUser.getId());

        // Assert
        assertEquals(1, (short)newCount);
    }
}