package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserV1Dto {

    public record RegisterRequest(
            @NotBlank(message = "ID는 필수입니다.")
            @Pattern(regexp = "^[a-zA-Z0-9]{1,10}$", message = "ID는 영문 및 숫자 10자 이내여야 합니다.")
            String id,

            @NotBlank(message = "이메일은 필수입니다.")
            @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "이메일은 xx@yy.zz 형식이어야 합니다.")
            String email,

            @NotBlank(message = "생년월일은 필수입니다.")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다.")
            String birthDate,

            @NotBlank(message = "성별은 필수입니다.")
            @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE 또는 FEMALE이어야 합니다.")
            String gender
    ) {
    }

    public record UserResponse(
            String id,
            String email,
            String birthDate,
            String gender
    ) {
        /**
         * Creates a UserResponse populated with values from the given UserInfo.
         *
         * @param info the source UserInfo containing id, email, birthDate, and gender
         * @return a UserResponse containing the same id, email, birthDate, and gender as the source
         */
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.id(),
                    info.email(),
                    info.birthDate(),
                    info.gender()
            );
        }
    }
}