package com.smarthealthdog.backend.services;

import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    // FIX THIS: Add logic to add permissions associated with a role to the UserDetails
    // For now, we just return an empty list of authorities.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check if "username" is actually a user ID
        if (username.matches("\\d+")) {
            Long userId = Long.parseLong(username);
            Optional<User> userOptionalById = userRepository.findById(userId);
            if (userOptionalById.isPresent()) {
                User user = userOptionalById.get();
                return new org.springframework.security.core.userdetails.User(
                    user.getId().toString(),
                    user.getPassword(),
                    Collections.emptyList()
                );
            }

            throw new UsernameNotFoundException("Could not find user with ID: " + username);
        }
        
        // Otherwise, treat "username" as an email
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("Could not find user with email: " + username);
        }

        User user = userOptional.get();
        return new org.springframework.security.core.userdetails.User(
            user.getId().toString(),
            user.getPassword(),
            Collections.emptyList()
        );
    }
}
