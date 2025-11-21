package com.loopers.domain.user;


import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;


@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");

    @Column(name = "user_id", unique = true, nullable = false, length = 10)
    private String userId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, length = 100))
    private Email email;

    @Embedded
    private BirthDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    private User(String userId, Email email, BirthDate birthDate, Gender gender) {
        validateUserId(userId);
        validateRequiredFields(userId, email, birthDate, gender);
        this.userId = userId;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public static User create(String userId, String email, String birthDate, Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수입니다.");
        }
        return new User(
                userId,
                new Email(email),
                new BirthDate(birthDate),
                gender
        );
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 비어있을 수 없습니다.");
        }
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자 10자 이내여야 합니다.");
        }
    }

    private void validateRequiredFields(String userId, Email email, BirthDate birthDate, Gender gender) {
        if (userId == null || email == null || birthDate == null || gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 필드가 누락되었습니다.");
        }
    }

    public String getUserIdValue() {
        return userId;
    }

    public String getEmailValue() {
        return email.getValue();
    }

    public String getBirthDateValue() {
        return birthDate.getValue();
    }
}
