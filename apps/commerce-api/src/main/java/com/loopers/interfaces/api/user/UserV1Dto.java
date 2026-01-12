package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import jakarta.validation.constraints.NotBlank;

public class UserV1Dto {
    public record UserResponse(String userId, String email, String birthDate) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                info.userId(),
                info.email(),
                info.birthDate()
            );
        }
    }

    public record SignupRequest(
        @NotBlank(message = "userId는 필수입니다.")
        String userId,
        @NotBlank(message = "email은 필수입니다.")
        String email,
        @NotBlank(message = "gender는 필수입니다.")
        String gender,
        @NotBlank(message = "birthDate는 필수입니다.")
        String birthDate
    ) {}
}
