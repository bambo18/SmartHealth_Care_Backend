package com.smarthealthdog.backend.validation;

import org.springframework.stereotype.Component;

@Component
public class NicknameValidator {
    public boolean isValid(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 128) {
            return false; // Length requirement
        }

        for (char ch : nickname.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') {
                return false; // Invalid character found
            }
        }

        return true; // All conditions met
    } 
}
