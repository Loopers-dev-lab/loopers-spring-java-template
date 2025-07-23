package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import jakarta.validation.constraints.NotNull;

public class UserV1Dto {
    public record CreateUserRequest(
            @NotNull
            String loginId,
            @NotNull
            String email,
            @NotNull
            String birth,
            @NotNull
            String gender
    ){}
    public record CreateUserResponse(
            Long userId,
            String loginId,
            String email,
            String birth,
            String gender
    ) {
        public static CreateUserResponse from(UserCommand.UserResponse userResponse) {
            return new CreateUserResponse(
                    userResponse.userId(),
                    userResponse.loginId(),
                    userResponse.email(),
                    userResponse.birth(),
                    userResponse.gender()
            );
        }
    }
    public record GetMeResponse(
            Long userId,
            String loginId,
            String email,
            String birth,
            String gender
    ) {
        public static GetMeResponse from(UserCommand.UserResponse userResponse) {
            return new GetMeResponse(
                    userResponse.userId(),
                    userResponse.loginId(),
                    userResponse.email(),
                    userResponse.birth(),
                    userResponse.gender()
            );
        }
    }
}
