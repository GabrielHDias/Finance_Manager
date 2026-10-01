package io.github.gabrielhdias.financeManager.api.user;

import io.github.gabrielhdias.financeManager.api.user.dto.RegisterUserRequest;
import io.github.gabrielhdias.financeManager.api.user.dto.UserResponse;
import io.github.gabrielhdias.financeManager.domain.user.User;
import io.github.gabrielhdias.financeManager.domain.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(
        @Valid @RequestBody RegisterUserRequest request
    ) {
        User user = userService.register(
            request.name(),
            request.email(),
            request.password()
        );

        UserResponse response = UserMapper.toResponse(user);

        return ResponseEntity
            .status(201)
            .body(response);
    }
}
