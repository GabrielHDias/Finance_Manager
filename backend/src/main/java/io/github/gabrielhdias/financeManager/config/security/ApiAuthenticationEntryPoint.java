package io.github.gabrielhdias.financeManager.config.security;

import io.github.gabrielhdias.financeManager.api.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ApiAuthenticationEntryPoint
    implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(
        ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {

        ApiErrorResponse errorResponse =
            new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Autenticação necessária",
                request.getRequestURI(),
                Map.of()
            );

        response.setStatus(
            HttpStatus.UNAUTHORIZED.value()
        );

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
            response.getOutputStream(),
            errorResponse
        );
    }
}
