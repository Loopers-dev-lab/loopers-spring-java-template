package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {
    @Column(name = "user_id", unique = true, nullable = false, length = 10)
    private String userId;

    private String email;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 필수입니다.");
        }
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자 10자 이내여야 합니다.");
        }
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수입니다.");
        }
        try {
            LocalDate.parse(birthDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    public User (String userId, String email, String birthDateStr, Gender gender) {
        validateUserId(userId);
        validateEmail(email);
        validateBirthDate(birthDateStr);

        this.userId = userId;
        this.email = email;
        this.birthDate = LocalDate.parse(birthDateStr);
        this.gender = gender;
    }

    public static User of(String userId, String email, String birthDate, Gender gender) {
        return new User(userId, email, birthDate, gender);
    }

}
