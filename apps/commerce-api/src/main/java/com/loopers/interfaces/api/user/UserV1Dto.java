package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserV1Dto {
    public record UserCreateRequest(
            @NotBlank
            @Pattern(regexp = "^[A-Za-z0-9]{1,10}$", message = "아이디는 영문 및 숫자 10자 이내여야 합니다.")
            String userId,

            @NotBlank
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            @NotBlank
            String birthDate,

            @NotNull(message = "성별은 필수 항목입니다.")
            Gender gender) {

    }

    public record UserResponse(String userId, String email, String birthDate, Gender gender) {
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
