package com.loopers.interfaces.api.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.loopers.application.user.UserResult;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public class UserDto {
    private static final String VALID_USER_ID_PATTERN = "^[A-Za-z0-9]{1,10}$";
    private static final String VALID_EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String YYYY_MM_DD = "yyyy-MM-dd";

    public record RegisterRequest(
        @NotBlank(message = "사용자 ID는 비어있을 수 없습니다.")
        @Pattern(regexp = VALID_USER_ID_PATTERN, message = "사용자 ID 형식이 올바르지 않습니다.")
        String userId,

        @NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @Pattern(regexp = VALID_EMAIL_PATTERN, message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotNull(message = "생년월일은 비어있을 수 없습니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        @JsonFormat(pattern = YYYY_MM_DD)
        LocalDate birth,

        @NotNull(message = "성별은 비어있을 수 없습니다.")
        Gender gender
    ) {}

    public record UserResponse(
        String userId,
        String email,
        @JsonFormat(pattern = YYYY_MM_DD)
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
