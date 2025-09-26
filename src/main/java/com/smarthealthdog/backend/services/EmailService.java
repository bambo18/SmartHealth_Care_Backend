package com.smarthealthdog.backend.services;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.utils.TokenGenerator;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TokenGenerator tokenGenerator;
    
    @Autowired
    private UserRepository userRepository;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.token.email-verification.expiration.minutes}")
    private int emailVerificationExpiryMinutes;

    @Async
    public void sendEmailVerification(User user) {
        String verificationCode = tokenGenerator.generateEmailVerificationCode();
        user.setEmailVerificationToken(verificationCode);

        Date now = new Date();
        user.setEmailVerificationRequestedAt(now.toInstant());
        user.setEmailVerificationExpiry(now.toInstant().plusSeconds(emailVerificationExpiryMinutes * 60L));
        userRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("[똑똑하개 건강하개] 이메일 인증 코드");
        message.setText("이메일 인증 코드는 " + verificationCode + " 입니다. 이 코드는 " + emailVerificationExpiryMinutes + "분 후에 만료됩니다.");
        message.setFrom(from);

        mailSender.send(message);
    }
}
