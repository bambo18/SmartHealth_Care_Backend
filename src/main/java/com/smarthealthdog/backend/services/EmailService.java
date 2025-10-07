package com.smarthealthdog.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.smarthealthdog.backend.domain.EmailVerification;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.token.email-verification.expiration.minutes}")
    private int emailVerificationExpiryMinutes;

    /**
     * 이메일 인증 메일 발송
     * @param user
     */
    @Async
    public void sendEmailVerification(String email, String token, EmailVerification emailVerification) {
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[똑똑하개 건강하개] 이메일 인증 코드");
        message.setText("이메일 인증 코드는 " + token + " 입니다. 이 코드는 " + emailVerificationExpiryMinutes + "분 후에 만료됩니다.");
        message.setFrom(from);

        mailSender.send(message);
    }
}
