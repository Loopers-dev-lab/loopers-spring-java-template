package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserInfo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserV1Dto {

    public record SignupRequest(
            @NotBlank(message = "로그인 ID는 필수입니다")
            String loginId,

            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "생년월일은 필수입니다")
            String birthDate,

            @NotBlank(message = "성별은 필수입니다")
            String gender
    ) {
        public UserCommand.SignupCommand toCommand() {
            return new UserCommand.SignupCommand(
                    loginId,
                    email,
                    birthDate,
                    gender
            );
        }
    }

    public record UserResponse(Long id, String loginId, String email, String birthDate, String gender) {
        public static UserV1Dto.UserResponse from(UserInfo info) {
            return new UserV1Dto.UserResponse(
                    info.id(),
                    info.loginId(),
                    info.email(),
                    info.birthDate().toString(),
                    info.gender()
            );
        }
    }

}
