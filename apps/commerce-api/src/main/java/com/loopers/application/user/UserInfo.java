package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

public record UserInfo(Long id, String userId, String email, String birthday, Gender gender) {
    public static UserInfo from(final User model) {
        return new UserInfo(
                model.getId(),
                model.getUserId(),
                model.getEmail(),
                Optional.ofNullable(model.getBirthday())
                        .map(LocalDate::toString)
                        .orElse(null),
                model.getGender()
        );
    }

    public User toModel() {
        return User.create(
                userId,
                email,
                parseBirthday(birthday),
                gender
        );
    }

    private LocalDate parseBirthday(String birthday) {
        if (Objects.isNull(birthday)) {
            return null;
        }

        try {
            return LocalDate.parse(birthday);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일의 형식이 맞지 않습니다.");
        }
    }
}
