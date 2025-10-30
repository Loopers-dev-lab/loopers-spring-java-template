package com.loopers.interfaces.api.signup;

import com.loopers.application.signup.SignUpInfo;
import jakarta.validation.constraints.NotBlank;

public class SignUpV1Dto {
    public record SignUpRequest(
        @NotBlank String userId,
        @NotBlank String email,
        @NotBlank String birthDate,
        @NotBlank String gender
    ) {}

    public record SignupResponse(Long id, String userId, String email, String birthDate, String gender) {
        public static SignupResponse from(SignUpInfo info) {
            return new SignupResponse(
                info.id(),
                info.userId(),
                info.email(),
                info.birthDate().toString(),
                info.gender() != null ? info.gender().name() : null
            );
        }
    }
}