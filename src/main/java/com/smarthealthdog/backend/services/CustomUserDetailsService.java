package com.smarthealthdog.backend.services;

import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.exceptions.BadCredentialsException;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.validation.ErrorCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional;
        List<GrantedAuthority> authorities = new ArrayList<>();

        // If the username consists only of digits, treat it as a user ID
        if (username.matches("\\d+")) {
            Long userId = Long.parseLong(username);
            userOptional = userRepository.findUserWithRoleAndPermissionsById(userId);
        } else {
            userOptional = userRepository.findByEmail(username);
        }
        
        if (!userOptional.isPresent()) {
            throw new BadCredentialsException(ErrorCode.LOGIN_FAILURE);
        }

        User user = userOptional.get();

        // 이미 로그인된 유저라면, 권한 정보를 UserDetails에 추가 
        if (username.matches("\\d+")) {
            if (user.getRole() == null) {
                throw new BadCredentialsException(ErrorCode.LOGIN_FAILURE);
            }

            // 밴되거나 삭제된 유저는 로그인 불가
            if (user.getRole().getName() == RoleEnum.BANNED_USER && 
                user.getRole().getName() == RoleEnum.DELETED_USER) {
                throw new BadCredentialsException(ErrorCode.LOGIN_FAILURE);
            }

            if (user.getRole().getPermissions() != null && !user.getRole().getPermissions().isEmpty()) {
                user.getRole().getPermissions().forEach(permission -> {
                    authorities.add(new SimpleGrantedAuthority(permission.getName().getName()));
                });
            }
        } else {
            List<RoleEnum> loginProhibitedRoles = List.of(
                RoleEnum.BANNED_USER,
                RoleEnum.DELETED_USER,
                RoleEnum.SOCIAL_ACCOUNT_USER
            );

            if (user.getRole() == null || loginProhibitedRoles.contains(user.getRole().getName())) {
                throw new BadCredentialsException(ErrorCode.LOGIN_FAILURE);
            }
        }

        return new org.springframework.security.core.userdetails.User(
            user.getId().toString(),
            user.getPassword(),
            authorities
        );
    }
}
