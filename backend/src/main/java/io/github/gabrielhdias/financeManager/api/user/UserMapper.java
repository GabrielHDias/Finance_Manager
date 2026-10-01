package io.github.gabrielhdias.financeManager.api.user;

import io.github.gabrielhdias.financeManager.api.user.dto.UserResponse;
import io.github.gabrielhdias.financeManager.domain.user.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail()
        );
    }
}
