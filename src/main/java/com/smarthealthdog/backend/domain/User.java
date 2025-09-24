package com.smarthealthdog.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, length = 256, unique = true)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "nickname", nullable = false, length = 128)
    private String nickname;

    @Column(name = "profile_pic", columnDefinition = "TEXT")
    private String profilePic;

    @Column(
        name = "login_attempt", 
        nullable = false,
        columnDefinition = "SMALLINT DEFAULT 0"
    )
    private Short loginAttempt;

    @Column(name = "login_attempt_started_at")
    private Timestamp loginAttemptStartedAt;

    @Column(name = "password_reset_token")
    private UUID passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private Timestamp passwordResetTokenExpiry;
    
    @Column(name = "password_reset_requested_at")
    private Timestamp passwordResetRequestedAt;

    @Column(name = "password_reset_token_verify_fail_count")
    private Short passwordResetTokenVerifyFailCount;

    @Column(name = "email_verification_token")
    private UUID emailVerificationToken;

    @Column(name = "email_verification_expiry")
    private Timestamp emailVerificationExpiry;

    @Column(name = "email_verification_requested_at")
    private Timestamp emailVerificationRequestedAt;

    @Column(name = "email_verification_fail_count")
    private Short emailVerificationFailCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;
}