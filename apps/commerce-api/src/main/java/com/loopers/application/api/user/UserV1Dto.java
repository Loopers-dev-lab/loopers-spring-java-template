package com.loopers.application.api.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.service.user.command.JoinUserCommand;
import com.loopers.core.service.user.command.UserPointChargeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
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
                    user.getId().value(),
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
                    user.getId().value(),
                    user.getIdentifier().value(),
                    user.getEmail().value(),
                    user.getBirthDay().value().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    user.getGender().name()
            );
        }
    }

    public record GetUserPointResponse(
            BigDecimal balance
    ) {

        public static GetUserPointResponse from(UserPoint userPoint) {
            return new GetUserPointResponse(userPoint.getBalance().value());
        }
    }

    public record UserPointChargeRequest(
            @Positive BigDecimal point
    ) {

        public UserPointChargeCommand toCommand(String userIdentifier) {
            return new UserPointChargeCommand(userIdentifier, point);
        }
    }

    public record UserPointChargeResponse(
            BigDecimal balance
    ) {

        public static UserPointChargeResponse from(UserPoint userPoint) {
            return new UserPointChargeResponse(userPoint.getBalance().value());
        }
    }
}
