package io.github.gabrielhdias.financeManager.api.auth;

import io.github.gabrielhdias.financeManager.api.auth.dto.LoginRequest;
import io.github.gabrielhdias.financeManager.api.auth.dto.LoginResponse;
import io.github.gabrielhdias.financeManager.domain.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(
    name = "Authentication",
    description = "Endpoints de autenticação"
)
public class AuthController {

    private final AuthService authService;

    public AuthController(
        AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Autenticar usuário",
        description = "Valida email e senha e retorna um token JWT"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Autenticação realizada com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de autenticação inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Email ou senha inválidos"
        )
    })
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
            authService.login(
                request.email(),
                request.password()
            );

        return ResponseEntity.ok(response);
    }
}
