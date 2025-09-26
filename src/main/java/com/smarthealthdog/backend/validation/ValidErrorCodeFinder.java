package com.smarthealthdog.backend.validation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Component
public class ValidErrorCodeFinder {
    public List<ErrorCode> findErrorCode(MethodArgumentNotValidException e) {
        // 에러가 발생한 객체의 클래스 이름을 가져옴
        String target = e.getBindingResult().getTarget().getClass().getSimpleName();

        switch (target) {
            case "UserCreateRequest":
                return UserCreateRequestErrorCode.getErrorCode(e);
            default:
                return List.of(ErrorCode.INVALID_INPUT);
        }
    }
}
