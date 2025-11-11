package com.smarthealthdog.backend.handlers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.smarthealthdog.backend.dto.ErrorMessage;
import com.smarthealthdog.backend.exceptions.ForbiddenException;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.validation.ErrorCode;
import com.smarthealthdog.backend.validation.ValidErrorCodeFinder;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private ValidErrorCodeFinder validErrorCodeFinder;

    // Spring Security InternalAuthenticationServiceException 예외 처리
    // 사용자의 정보를 로드할 수 없을 때 발생
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessage handleInternalAuthenticationServiceException(Exception e) {
        return new ErrorMessage(
            List.of(ErrorCode.LOGIN_FAILURE.name()),
            List.of(ErrorCode.LOGIN_FAILURE.getMessage())
        );
    }

    // JWT 관련 예외 처리 - MalformedJwtException, ExpiredJwtException, UnsupportedJwtException
    @ExceptionHandler(
        value = {
            MalformedJwtException.class,
            ExpiredJwtException.class,
            UnsupportedJwtException.class,
            BadCredentialsException.class,
            com.smarthealthdog.backend.exceptions.BadCredentialsException.class
        }
    )
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessage handleJwtExceptions(Exception e) {
        if (e instanceof com.smarthealthdog.backend.exceptions.BadCredentialsException) {
            return new ErrorMessage(
                List.of(((com.smarthealthdog.backend.exceptions.BadCredentialsException) e).getErrorCode().name()),
                List.of(((com.smarthealthdog.backend.exceptions.BadCredentialsException) e).getErrorCode().getMessage())
            );
        }

        return new ErrorMessage(
            List.of(ErrorCode.INVALID_JWT.name()),
            List.of(ErrorCode.INVALID_JWT.getMessage())
        );
    }

    // 404 에러 처리 - ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorMessage> handleResourceNotFoundException(ResourceNotFoundException e) {
        ErrorMessage errorResponseBody = new ErrorMessage(
            List.of(e.getErrorCode().name()),
            List.of(e.getErrorCode().getMessage())
        );
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponseBody);
    }

    // 400 에러 처리 - MethodArgumentNotValidException
    // @Valid 검증 실패 시 발생을 핸들링
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorMessage> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ErrorCode> errorCodes = validErrorCodeFinder.findErrorCode(e);
        List<String> messages = null;

        if (errorCodes.size() > 1) {
            messages = e.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .distinct()
                .toList();
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorMessage(
                errorCodes.stream().map(ErrorCode::name).toList(),
                messages != null ? messages : List.of(errorCodes.get(0).getMessage())
            ));
    }

    // 400 에러 처리
    @ExceptionHandler(InvalidRequestDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorMessage> handleInvalidRequestDataException(InvalidRequestDataException e) {
        ErrorMessage errorResponseBody = new ErrorMessage(
            List.of(e.getErrorCode().name()),
            List.of(e.getErrorCode().getMessage())
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponseBody);
    }

    // 403 에러 처리
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorMessage> handleForbiddenException(ForbiddenException e) {
        ErrorMessage errorResponseBody = new ErrorMessage(
            List.of(e.getErrorCode().name()),
            List.of(e.getErrorCode().getMessage())
        );

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(errorResponseBody);
    }

    // 나머지 예외 처리
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorMessage> handleGenericException(Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorMessage(
                List.of(ErrorCode.INTERNAL_SERVER_ERROR.name()),
                List.of("서버에서 예기치 않은 오류가 발생했습니다.")
            ));
    }
}
