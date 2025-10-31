package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(
        String id,
        String email,
        String birthDate,
        String gender
) {
    /**
     * Creates a UserInfo DTO from a domain User by copying id, email, birthDate, and gender.
     *
     * @param user the domain User to convert
     * @return a new UserInfo containing the user's id, email, birthDate, and gender
     */
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender()
        );
    }
}