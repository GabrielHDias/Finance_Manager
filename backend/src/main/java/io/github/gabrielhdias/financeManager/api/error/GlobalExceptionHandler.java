package io.github.gabrielhdias.financeManager.api.error;

import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.DuplicateResourceException;
import io.github.gabrielhdias.financeManager.domain.exception.InvalidCredentialsException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fields = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.put(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            );
        }

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            "Dados inválidos",
            request,
            fields
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
        IllegalStateException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "Conflict",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
        DuplicateResourceException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "Conflict",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleException(
        BusinessRuleException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "Conflict",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentialsException(
        InvalidCredentialsException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String error,
        String message,
        HttpServletRequest request,
        Map<String, String> fields
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            LocalDateTime.now(),
            status.value(),
            error,
            message,
            request.getRequestURI(),
            fields
        );

        return ResponseEntity
            .status(status)
            .body(response);
    }
}
