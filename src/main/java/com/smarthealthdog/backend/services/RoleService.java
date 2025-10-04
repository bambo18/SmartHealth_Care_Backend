package com.smarthealthdog.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.domain.RoleEnum;
import com.smarthealthdog.backend.repositories.RoleRepository;

@Service
public class RoleService {
    private RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * 인증되지 않은 유저 역할 조회
     * @return Role 객체
     */
    public Role getUnverifiedUserRole() {
        return roleRepository.findByName(RoleEnum.UNVERIFIED_USER)
            .orElseThrow(() -> new RuntimeException("Role 'UNVERIFIED_USER' not found"));
    }

    /**
     * 일반 유저 역할 조회
     * @return Role 객체
     */
    public Role getUserRole() {
        return roleRepository.findByName(RoleEnum.USER)
            .orElseThrow(() -> new RuntimeException("Role 'USER' not found"));
    }
}