package com.loopers.application.api.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.loopers.core.domain.user.User;
import com.loopers.core.service.user.command.JoinUserCommand;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class UserV1Dto {

    public record JoinUserRequest(
            @NotBlank String identifier,
            @NotBlank String email,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birthday,
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
