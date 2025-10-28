package com.loopers.application.api.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.service.user.command.JoinUserCommand;
import jakarta.validation.constraints.NotBlank;

import java.time.format.DateTimeFormatter;

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

    public record GetUserResponse(
            String userId,
            String identifier,
            String email,
            String birthday,
            String gender
    ) {

        public static GetUserResponse from(User user) {
            return new GetUserResponse(
                    user.getUserId().value(),
                    user.getIdentifier().value(),
                    user.getEmail().value(),
                    user.getBirthDay().value().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    user.getGender().name()
            );
        }
    }

    public record JoinUserResponse(
            String userId,
            String identifier,
            String email,
            String birthday,
            String gender
    ) {

        public static JoinUserResponse from(User user) {
            return new JoinUserResponse(
                    user.getUserId().value(),
                    user.getIdentifier().value(),
                    user.getEmail().value(),
                    user.getBirthDay().value().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    user.getGender().name()
            );
        }
    }
}
