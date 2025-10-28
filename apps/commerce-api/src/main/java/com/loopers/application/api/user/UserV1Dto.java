package com.loopers.application.api.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.service.user.command.JoinUserCommand;
import jakarta.validation.constraints.NotBlank;

public class UserV1Dto {

    public record JoinUserRequest(
            @NotBlank String identifier,
            @NotBlank String email,
            @NotBlank String birthday,
            @NotBlank String gender
    ) {
        public JoinUserCommand toCommand() {
            return new JoinUserCommand(identifier, email, birthday, gender);
        }
    }

    public record GetUserResponse(String userId) {

    }

    public record JoinUserResponse(String userId) {

        public static JoinUserResponse from(User user) {
            return new JoinUserResponse(user.getUserId().value());
        }
    }
}
