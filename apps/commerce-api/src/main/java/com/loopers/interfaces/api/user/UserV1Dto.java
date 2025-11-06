package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserV1Dto {
    public record UserSignUpRequest(
            @NotBlank(message = "Id를 입력해 주세요.")
            String userId,
            String email,
            String birthday,
            @NotNull(message = "성별을 선택해 주세요.")
            Gender gender
    ) {
        public UserInfo toInfo() {
            return new UserInfo(null, userId, email, birthday, gender);
        }
    }

    public record UserSignUpResponse(Long id, String userId, String email, String birthday) {
        public static UserSignUpResponse from(UserInfo info) {
            return new UserSignUpResponse(
                    info.id(),
                    info.userId(),
                    info.email(),
                    info.birthday()
            );
        }
    }

    public record UserResponse(Long id, String userId, String email, String birthday) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.id(),
                    info.userId(),
                    info.email(),
                    info.birthday()
            );
        }
    }
}
