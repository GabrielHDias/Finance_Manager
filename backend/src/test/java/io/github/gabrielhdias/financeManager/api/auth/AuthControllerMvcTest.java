package io.github.gabrielhdias.financeManager.api.auth;

import io.github.gabrielhdias.financeManager.api.auth.dto.LoginResponse;
import io.github.gabrielhdias.financeManager.config.security.SecurityConfig;
import io.github.gabrielhdias.financeManager.domain.auth.AuthService;
import io.github.gabrielhdias.financeManager.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldLoginWithoutAuthentication() throws Exception {
        when(authService.login(
            "gabriel@email.com",
            "senha123"
        )).thenReturn(
            new LoginResponse(
                "jwt-token",
                "Bearer",
                3600L
            )
        );

        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "email": "gabriel@email.com",
                                  "password": "senha123"
                                }
                                """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken")
                .value("jwt-token"))
            .andExpect(jsonPath("$.tokenType")
                .value("Bearer"))
            .andExpect(jsonPath("$.expiresIn")
                .value(3600));
    }

    @Test
    void shouldReturnBadRequestWhenLoginRequestIsInvalid() throws Exception {
        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "email": "email-invalido",
                                  "password": ""
                                }
                                """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status")
                .value(400))
            .andExpect(jsonPath("$.error")
                .value("Validation Error"))
            .andExpect(jsonPath("$.message")
                .value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.email")
                .exists())
            .andExpect(jsonPath("$.fields.password")
                .exists());
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(authService.login(
            "gabriel@email.com",
            "senhaErrada"
        )).thenThrow(
            new InvalidCredentialsException(
                "Email ou senha inválidos"
            )
        );

        mockMvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "email": "gabriel@email.com",
                                  "password": "senhaErrada"
                                }
                                """)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status")
                .value(401))
            .andExpect(jsonPath("$.error")
                .value("Unauthorized"))
            .andExpect(jsonPath("$.message")
                .value("Email ou senha inválidos"));
    }
}
