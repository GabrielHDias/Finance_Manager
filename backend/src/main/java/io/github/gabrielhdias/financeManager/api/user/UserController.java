package io.github.gabrielhdias.financeManager.api.user;

import io.github.gabrielhdias.financeManager.api.user.dto.EmailChangeRequest;
import io.github.gabrielhdias.financeManager.api.user.dto.PasswordConfirmationRequest;
import io.github.gabrielhdias.financeManager.api.user.dto.RegisterUserRequest;
import io.github.gabrielhdias.financeManager.api.user.dto.UserResponse;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final AuthenticatedUser authenticatedUser;

    public UserController(
        UserService userService,
        AuthenticatedUser authenticatedUser
    ) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;
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

    @GetMapping("/me")
    public UserResponse me() {
        return UserMapper.toResponse(userService.findById(authenticatedUser.getId()));
    }

    @PutMapping("/me/email")
    public UserResponse changeEmail(@Valid @RequestBody EmailChangeRequest request) {
        return UserMapper.toResponse(userService.changeEmail(
            authenticatedUser.getId(), request.email(), request.currentPassword()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(@Valid @RequestBody PasswordConfirmationRequest request) {
        userService.deleteOwnAccount(authenticatedUser.getId(), request.currentPassword());
        return ResponseEntity.noContent().build();
    }
}
