package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils; // Utility to set @Value fields

import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.utils.TokenGenerator;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    // InjectMocks attempts to inject the mocks into this instance
    @InjectMocks
    private EmailService emailService;

    // Mock all the dependencies
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TokenGenerator tokenGenerator;
    @Mock
    private UserRepository userRepository;

    // Fixed test data
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_CODE = "XYZ123";
    private final String TEST_FROM = "no-reply@smarthealthdog.com";
    private final int TEST_EXPIRY_MINUTES = 30;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Use ReflectionTestUtils to set the @Value annotated fields
        ReflectionTestUtils.setField(emailService, "from", TEST_FROM);
        ReflectionTestUtils.setField(emailService, "emailVerificationExpiryMinutes", TEST_EXPIRY_MINUTES);

        // Create a user object for testing
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(TEST_EMAIL);

        // Stub the token generator to return a predictable value
        when(tokenGenerator.generateEmailVerificationCode()).thenReturn(TEST_CODE);
    }

    @Test
    void sendEmailVerification_ShouldUpdateUserAndSendEmail() {
        // ARRANGE: Use an ArgumentCaptor to capture the User object when save() is called
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        // ARRANGE: Use an ArgumentCaptor to capture the SimpleMailMessage when send() is called
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // ACT
        // Note on @Async: In a unit test, calling the method directly executes it synchronously.
        // We verify the side effects (save and send) as if it ran successfully.
        emailService.sendEmailVerification(testUser);

        // 1. VERIFY User was updated and saved to the repository
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // ASSERT 1: Verify User State Changes
        assertEquals(TEST_CODE, savedUser.getEmailVerificationToken());
        assertNotNull(savedUser.getEmailVerificationRequestedAt());
        assertNotNull(savedUser.getEmailVerificationExpiry());
        
        // Verify expiry time (the exact Instant comparison is complex, so we check general direction)
        Instant requestedTime = savedUser.getEmailVerificationRequestedAt();
        Instant expectedExpiry = requestedTime.plusSeconds(TEST_EXPIRY_MINUTES * 60L);
        assertEquals(expectedExpiry.getEpochSecond(), savedUser.getEmailVerificationExpiry().getEpochSecond(), 1, 
                     "Expiry time should be approximately 30 minutes after request time.");


        // 2. VERIFY Email was sent by JavaMailSender
        // Use verify and captor to check the contents of the email message
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        // ASSERT 2: Verify Email Contents
        assertEquals(TEST_FROM, sentMessage.getFrom());
        assertEquals(TEST_EMAIL, sentMessage.getTo()[0]);
        assertEquals("[똑똑하개 건강하개] 이메일 인증 코드", sentMessage.getSubject());
        
        // Check the body contains the code and expiry time
        String expectedText = "이메일 인증 코드는 " + TEST_CODE + " 입니다. 이 코드는 " + TEST_EXPIRY_MINUTES + "분 후에 만료됩니다.";
        assertEquals(expectedText, sentMessage.getText());
    }
}