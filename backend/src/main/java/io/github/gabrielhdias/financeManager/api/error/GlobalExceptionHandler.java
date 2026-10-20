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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleLargeUpload(HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large",
            "O arquivo deve ter no máximo 1 MiB.", request, Map.of());
    }

    @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleMissingImportPart(HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
            "Parâmetros obrigatórios ausentes", request, Map.of());
    }

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

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    public ResponseEntity<ApiErrorResponse> malformed(HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "JSON ou parâmetros inválidos", request, Map.of());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> integrity(org.springframework.dao.DataIntegrityViolationException failure,
                                                     HttpServletRequest request) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if (java.util.Set.of("uk_users_email", "uk_accounts_user_name", "uk_categories_user_name",
                    "uk_budgets_category_month").contains(name == null ? "" : name))
                    return buildResponse(HttpStatus.CONFLICT, "Conflict", "Recurso já cadastrado", request, Map.of());
                if (java.util.Set.of("fk_transactions_account", "fk_transactions_category", "fk_accounts_user",
                    "fk_categories_user", "monthly_budgets_category_id_fkey").contains(name == null ? "" : name))
                    return buildResponse(HttpStatus.CONFLICT, "Conflict", "Recurso possui vínculos existentes", request, Map.of());
            }
            cause = cause.getCause();
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Falha interna de persistência", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String error,
        String message,
        HttpServletRequest request,
        Map<String, String> fields
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
            Instant.now(),
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
