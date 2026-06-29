package com.onlyfriends.common.exception;

import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex) {
        return ResponseEntity
                .status(toHttpStatus(ex.getCode()))
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new Result<>(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage(), data, System.currentTimeMillis()));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ResultCode.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("Unhandled server exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }

    private Map<String, String> toFieldError(FieldError error) {
        Map<String, String> item = new HashMap<>();
        item.put("field", error.getField());
        item.put("message", error.getDefaultMessage());
        return item;
    }

    private HttpStatus toHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401, 1007 -> HttpStatus.UNAUTHORIZED;
            case 403, 1002, 1003 -> HttpStatus.FORBIDDEN;
            case 404, 1001 -> HttpStatus.NOT_FOUND;
            case 1004, 1005, 1101, 1102 -> HttpStatus.CONFLICT;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        };
    }
}
