package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserDomainCreateRequest;

public record UserRegisterCommand(
        String username,
        String email,
        String birthdate,
        Gender gender
) {
    public static UserRegisterCommand of(String username, String email, String birthdate, Gender gender) {
        return new UserRegisterCommand(username, email, birthdate, gender);
    }

    public UserDomainCreateRequest toDomainRequest() {
        return UserDomainCreateRequest.of(username, email, birthdate, gender);
    }
}
