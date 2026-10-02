package io.github.gabrielhdias.financeManager.api.user;

import io.github.gabrielhdias.financeManager.api.user.dto.RegisterUserRequest;
import io.github.gabrielhdias.financeManager.api.user.dto.UserResponse;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(
    name = "Users",
    description = "Gerenciamento de usuários"
)
public class UserController {

    private final UserService userService;

    public UserController(
        UserService userService
    ) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(
        summary = "Cadastrar usuário",
        description = "Cria um novo usuário no Finance Manager"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Usuário criado com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de cadastro inválidos"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um usuário com o email informado"
        )
    })
    public ResponseEntity<UserResponse> register(
        @Valid @RequestBody RegisterUserRequest request
    ) {
        User user = userService.register(
            request.name(),
            request.email(),
            request.password()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(UserMapper.toResponse(user));
    }
}
