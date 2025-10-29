package com.loopers.interfaces.api.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.loopers.application.user.UserResult;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class UserDto {
    public record RegisterRequest(
        @NotBlank(message = "사용자 ID는 비어있을 수 없습니다.")
        String userId,

        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        String email,

        @NotNull(message = "생년월일은 비어있을 수 없습니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birth,

        @NotNull(message = "성별은 비어있을 수 없습니다.")
        Gender gender
    ) {}

    public record UserResponse(
        String userId,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birth,
        Gender gender
    ) {
        public static UserResponse from(UserResult info) {
            return new UserResponse(
                info.userId(),
                info.email(),
                info.birth(),
                info.gender()
            );
        }
    }
}
