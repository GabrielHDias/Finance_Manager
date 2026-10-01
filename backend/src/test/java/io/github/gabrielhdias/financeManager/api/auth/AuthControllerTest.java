package io.github.gabrielhdias.financeManager.api.auth;

import io.github.gabrielhdias.financeManager.api.auth.dto.LoginRequest;
import io.github.gabrielhdias.financeManager.api.auth.dto.LoginResponse;
import io.github.gabrielhdias.financeManager.domain.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldLogin() {
        LoginRequest request = new LoginRequest(
            "gabriel@email.com",
            "senha123"
        );

        LoginResponse loginResponse = new LoginResponse(
            "jwt-token",
            "Bearer",
            3600L
        );

        when(authService.login(
            request.email(),
            request.password()
        )).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response =
            authController.login(request);

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
            "jwt-token",
            response.getBody().accessToken()
        );

        assertEquals(
            "Bearer",
            response.getBody().tokenType()
        );

        assertEquals(
            3600L,
            response.getBody().expiresIn()
        );

        verify(authService).login(
            "gabriel@email.com",
            "senha123"
        );
    }
}
