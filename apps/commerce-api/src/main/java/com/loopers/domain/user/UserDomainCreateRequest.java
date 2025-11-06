package com.loopers.domain.user;

import jakarta.validation.constraints.NotNull;

/**
 * @author hyunjikoh
 * @since 2025. 10. 26.
 */
public record UserDomainCreateRequest(
        @NotNull
        String username,

        @NotNull
        String email,

        @NotNull
        String birthdate,

        @NotNull
        Gender gender
) {
    public static UserDomainCreateRequest of(String username, String email, String birthdate, Gender gender) {
        return new UserDomainCreateRequest(username, email, birthdate, gender);
    }
}
