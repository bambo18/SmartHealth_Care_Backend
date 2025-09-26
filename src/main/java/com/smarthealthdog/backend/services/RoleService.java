package com.smarthealthdog.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smarthealthdog.backend.domain.Role;
import com.smarthealthdog.backend.repositories.RoleRepository;

@Service
public class RoleService {
    private RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getUnverifiedUserRole() {
        return roleRepository.findByName("UNVERIFIED_USER")
            .orElseThrow(() -> new RuntimeException("Role 'UNVERIFIED_USER' not found"));
    }

    public Role getUserRole() {
        return roleRepository.findByName("USER")
            .orElseThrow(() -> new RuntimeException("Role 'USER' not found"));
    }
}