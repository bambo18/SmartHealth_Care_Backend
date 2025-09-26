package com.smarthealthdog.backend.validation;

import java.util.List;

import org.springframework.web.bind.MethodArgumentNotValidException;


public class UserCreateRequestErrorCode {
    public static final ErrorCode INVALID_NICKNAME = ErrorCode.INVALID_NICKNAME;
    public static final ErrorCode INVALID_PASSWORD = ErrorCode.INVALID_PASSWORD;
    public static final ErrorCode INVALID_EMAIL = ErrorCode.INVALID_EMAIL;

    private UserCreateRequestErrorCode() {
        // Prevent instantiation
    }

    public static List<ErrorCode> getErrorCode(MethodArgumentNotValidException e) {
        List<String> fields = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField())
            .toList();

        if (fields.isEmpty()) {
            return List.of(ErrorCode.INVALID_INPUT);
        }

        return fields.stream()
            .map(fieldName -> getErrorCode(fieldName))
            .distinct()
            .toList();
    }

    private static ErrorCode getErrorCode(String fieldName) {
        return switch (fieldName) {
            case "nickname" -> INVALID_NICKNAME;
            case "password" -> INVALID_PASSWORD;
            case "email" -> INVALID_EMAIL;
            default -> ErrorCode.INVALID_INPUT;
        };
    }
}
