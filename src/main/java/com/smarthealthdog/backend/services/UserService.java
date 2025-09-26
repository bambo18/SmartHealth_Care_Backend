package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.validation.ErrorCode;
import com.smarthealthdog.backend.validation.NicknameValidator;
import com.smarthealthdog.backend.validation.PasswordValidator;


@Service
public class UserService {
    private UserRepository userRepository;
    private RoleService roleService;
    private PasswordEncoder passwordEncoder;
    private PasswordValidator passwordValidator;
    private NicknameValidator nicknameValidator;

    @Autowired
    public UserService(
        UserRepository userRepository, 
        RoleService roleService,
        PasswordEncoder passwordEncoder,
        PasswordValidator passwordValidator,
        NicknameValidator nicknameValidator
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordValidator = passwordValidator;
        this.nicknameValidator = nicknameValidator;
    }

    public User createUser(String nickname, String email, String password) {
        boolean isValidNickname = nicknameValidator.isValid(nickname);
        if (!isValidNickname) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_NICKNAME);
        }

        boolean existingByEmail = userRepository.existsByEmail(email);
        if (existingByEmail) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_EMAIL);
        }

        boolean isValidPassword = passwordValidator.isValid(password);
        if (!isValidPassword) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_PASSWORD);
        }

        Role unverifiedUserRole = roleService.getUnverifiedUserRole();

        User newUser = new User();
        newUser.setNickname(nickname);
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setRole(unverifiedUserRole);

        setUserPassword(newUser, password);
        userRepository.save(newUser);

        return newUser;
    }

    public void deleteUser(Long id) {
        // Logic to delete a user
        userRepository.deleteById(id);
    }

    public Optional<User> getUserById(Long id) {
        // Logic to retrieve a user by ID
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        // Logic to retrieve a user by email
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByNickname(String nickname) {
        // Logic to retrieve a user by nickname
        return userRepository.findByNickname(nickname);
    }

    public boolean userExistsByEmail(String email) {
        // Logic to check if a user exists by email
        return userRepository.existsByEmail(email);
    }

    public boolean userExistsByNickname(String nickname) {
        // Logic to check if a user exists by nickname
        return userRepository.existsByNickname(nickname);
    }

    public boolean userExistsById(Long id) {
        // Logic to check if a user exists by ID
        return userRepository.existsById(id);
    }

    public void setUserPassword(User user, String rawPassword) {
        String hashedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(hashedPassword);
    }

    public boolean checkUserPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void changeUnverifiedUserToUser(User user) {
        Role userRole = roleService.getUserRole();
        user.setRole(userRole);
        userRepository.save(user);
    }

    public void expireEmailVerificationToken(User user) {
        Instant now = Instant.now();
        user.setEmailVerificationExpiry(now);
        userRepository.save(user);
    }

    @Transactional
    public void incrementEmailVerificationFailCount(User user) {
        userRepository.incrementEmailVerificationFailCount(user.getId());
    }
}
