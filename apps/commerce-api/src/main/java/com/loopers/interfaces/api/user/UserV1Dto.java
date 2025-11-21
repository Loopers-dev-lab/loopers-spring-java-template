package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserV1Dto {
    public record RegisterRequest(
            @NotBlank(message = "ID는 필수입니다.")
            @Pattern(regexp = "^[a-zA-Z0-9]{1,10}$", message = "ID는 영문 및 숫자 10자 이내여야 합니다.")
            String userId,

            @NotBlank(message = "이메일은 필수입니다.")
            @Pattern(regexp = "^[^@]+@[^@]+\\.[^@]+$", message = "이메일은 xx@yy.zz 형식이어야 합니다.")
            String email,

            @NotBlank(message = "생년월일은 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다.")
            String birthDate,

            @NotNull(message = "성별은 필수입니다.")
            Gender gender
    ) {
        public UserCommand toCommand() {
            return new UserCommand(
                    this.userId,
                    this.email,
                    this.birthDate,
                    this.gender
            );
        }
    }

    public record UserResponse(
            String userId,
            String email,
            String birthDate,
            Gender gender
    ) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.userId(),
                    info.email(),
                    info.birthDate(),
                    info.gender()
            );
        }
    }
}
