package com.smarthealthdog.backend.validation;

public enum ErrorCode {
    LOGIN_FAILURE("로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요."),
    INVALID_INPUT("잘못된 입력입니다."),
    RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("서버에서 예기치 않은 오류가 발생했습니다."),
    INVALID_JWT("잘못된 JWT 토큰이거나 만료되었습니다."),

    // 유저 생성 관련 오류
    INVALID_NICKNAME("닉네임은 3-128자여야 합니다."),
    INVALID_EMAIL("이미 사용 중인 이메일이거나 형식이 올바르지 않습니다."),
    INVALID_PASSWORD("비밀번호는 8-256자여야 하며, 최소 하나의 대문자, 소문자, 숫자 및 특수 문자(!@#$%^&*()-+)을 포함해야 합니다."),
    INVALID_EMAIL_VERIFICATION("이메일 인증 토큰이 만료되었거나 유효하지 않습니다."),

    EMAIL_VERIFICATION_TRIES_EXCEEDED("이메일 인증 시도 횟수를 초과했습니다. 하루 후 다시 시도해주세요."),
    EMAIL_VERIFICATION_FAIL_COUNT_EXCEEDED("이메일 인증 실패 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}