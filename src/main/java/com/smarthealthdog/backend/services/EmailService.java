package com.smarthealthdog.backend.services;

import com.smarthealthdog.backend.domain.EmailVerification;

public interface EmailService {
    /**
     * 이메일 인증 메일 발송 (비동기)
     * @param email
     * @param token
     * @param emailVerification
     */
    void sendEmailVerification(String email, String token, EmailVerification emailVerification);
}