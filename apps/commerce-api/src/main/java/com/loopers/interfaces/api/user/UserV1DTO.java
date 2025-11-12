package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UserV1DTO {

    public record UserRequest(
            @NotBlank(message = "사용자 ID는 필수값입니다.") String userId,
            @NotBlank(message = "이메일은 필수값입니다.") String email,
            @NotBlank(message = "생년월일은 필수값입니다.") String birthdate,
            @NotNull(message = "성별은 필수값입니다.") Gender gender
    ) { }

    public record UserPointRequest(
            @NotBlank(message = "사용자 ID는 필수값입니다.") String userId,
            BigDecimal chargePoint
    ) { }

    public record UserResponse(Long id, String userId, String email, String birthdate, Gender gender, BigDecimal point) {
        public static UserResponse from(UserInfo userInfo) {
            return new UserResponse(
                    userInfo.id(),
                    userInfo.userId(),
                    userInfo.email(),
                    userInfo.birthdate(),
                    userInfo.gender(),
                    userInfo.point()
            );
        }
    }

    public record UserPointResponse(String userId, BigDecimal point) {
        public static UserPointResponse from(String userId, BigDecimal point) {
            return new UserPointResponse(
                    userId,
                    point
            );
        }
    }
}
