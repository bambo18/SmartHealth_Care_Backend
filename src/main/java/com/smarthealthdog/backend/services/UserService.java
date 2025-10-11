package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.Optional;

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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final NicknameValidator nicknameValidator;

    /**
     * 사용자 비밀번호 확인
     * @param user
     * @param rawPassword
     * @return true 만약 비밀번호가 일치하는 경우, false otherwise
     */
    public boolean checkUserPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * 사용자 역할을 VERIFIED_USER로 변경
     * @param user
     */
    @Transactional
    public void changeRoleToVerifiedUser(User user) {
        Role userRole = roleService.getUserRole();
        user.setRole(userRole);
        userRepository.save(user);
    }

    /**
     * 새로운 사용자 생성
     * @param nickname 3-128 characters
     * @param email 유효한 이메일 형식
     * @param password 8-256자, 대문자, 소문자, 숫자, 특수문자 포함
     * @return 생성된 사용자 객체
     */
    @Transactional
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

        Role userRole = roleService.getUserRole();

        String hashedPassword = passwordEncoder.encode(password);
        User newUser = User.builder()
                            .nickname(nickname)
                            .email(email)
                            .password(hashedPassword)
                            .role(userRole)
                            .build();

        userRepository.save(newUser);
        return newUser;
    }

    /**
     * 사용자 삭제
     * @param id
     */
    @Transactional
    public void deleteUser(Long id) {
        // Logic to delete a user
        userRepository.deleteById(id);
    }

    /**
     * 이메일 인증 토큰 만료
     * @param user
     */
    @Transactional
    public void expireEmailVerificationToken(User user) {
        Instant now = Instant.now();
        user.setEmailVerificationExpiry(now);
        userRepository.save(user);
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

    /**
     * 이메일 인증 실패 횟수 증가
     * @param user
     */
    @Transactional
    public void incrementEmailVerificationFailCount(User user) {
        userRepository.incrementEmailVerificationFailCount(user.getId());
    }

    /**
     * 이메일 인증 실패 횟수 초기화
     * @param user
     */
    @Transactional
    public void resetEmailVerificationFailCount(User user) {
        userRepository.resetEmailVerificationFailCount(user.getId());
    }

    /**
     * 사용자 비밀번호 설정 (해싱 포함)
     * @param user
     * @param rawPassword
     */
    public void setUserPassword(User user, String rawPassword) {
        String hashedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(hashedPassword);
    }

    /**
     * 이메일로 사용자 존재 여부 확인
     * @param email
     * @return true if user exists, false otherwise
     */
    public boolean userExistsByEmail(String email) {
        // Logic to check if a user exists by email
        return userRepository.existsByEmail(email);
    }

    /**
     * 닉네임으로 사용자 존재 여부 확인
     * @param nickname
     * @return true if user exists, false otherwise
     */
    public boolean userExistsByNickname(String nickname) {
        // Logic to check if a user exists by nickname
        return userRepository.existsByNickname(nickname);
    }

    /**
     * ID로 사용자 존재 여부 확인
     * @param id
     * @return true if user exists, false otherwise
     */
    public boolean userExistsById(Long id) {
        // Logic to check if a user exists by ID
        return userRepository.existsById(id);
    }
}
