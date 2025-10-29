package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Table(name = "user")
@Getter
public class User extends BaseEntity {

    private static final int MAX_USER_ID_LENGTH = 10;
    private static final String USER_ID_PATTERN = "^[a-zA-Z0-9]+$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    protected User() {}

    public User(String userId, String email, LocalDate birth, Gender gender) {
        validateUserId(userId);
        validateEmail(email);
        validateBirth(birth);
        validateGender(gender);
        this.userId = userId;
        this.email = email;
        this.birth = birth;
        this.gender = gender;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 비어있을 수 없습니다.");
        }
        if (userId.length() > MAX_USER_ID_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 영문 및 숫자 10자 이내여야 합니다.");
        }
        if (!userId.matches(USER_ID_PATTERN)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 영문 및 숫자 10자 이내여야 합니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if (!email.matches(EMAIL_PATTERN)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }

    private void validateBirth(LocalDate birth) {
        if (birth == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
    }

    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 비어있을 수 없습니다.");
        }
    }
}
