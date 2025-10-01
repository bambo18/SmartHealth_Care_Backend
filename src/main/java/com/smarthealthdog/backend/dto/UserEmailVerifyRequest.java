package com.smarthealthdog.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserEmailVerifyRequest(
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "유효하지 않은 이메일 형식입니다.")
    String email, 

    @NotBlank(message = "토큰은 필수 입력값입니다.")
    @Size(min = 6, max = 6, message = "토큰은 6자여야 합니다.")
    String token
) {
}
