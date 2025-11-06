package com.loopers.interfaces.api.user;

import java.time.LocalDate;

import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserV1Dtos {
    public record UserRegisterRequest(
            @NotNull @Size(max = 10) @Pattern(regexp = "^[a-zA-Z0-9]+$")
            String username,

            @NotNull @Email
            String email,

            @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
            String birthdate,

            @NotNull
            Gender gender
    ) {
        public UserRegisterCommand toCommand() {
            return UserRegisterCommand.of(username, email, birthdate, gender);
        }
    }

    public record UserRegisterResponse(
            Long id,
            String username,
            String email,
            LocalDate birthdate,
            Gender gender
    ) {
        public static UserRegisterResponse from(UserInfo userInfo) {
            return new UserRegisterResponse(
                    userInfo.id(),
                    userInfo.username(),
                    userInfo.email(),
                    userInfo.birthdate(),
                    userInfo.gender()
            );
        }
    }

    public record UserInfoResponse(
            String username,
            String email,
            LocalDate birthdate,
            Gender gender
    ) {
        public static UserInfoResponse from(UserInfo userInfo) {
            return new UserInfoResponse(
                    userInfo.username(),
                    userInfo.email(),
                    userInfo.birthdate(),
                    userInfo.gender()
            );
        }
    }
}
