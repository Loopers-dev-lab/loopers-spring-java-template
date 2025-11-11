package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.error.ErrorMessages;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private String email;
    private String birthDate;

    @Builder
    public User(String userId, Gender gender, String email, String birthDate) {
        validateUserId(userId);
        validateGender(gender);
        validateEmail(email);
        validateBirthDate(birthDate);

        this.userId = userId;
        this.gender = gender;
        this.email = email;
        this.birthDate = birthDate;
    }

    private void validateUserId(String userId) {
        if (userId == null || !userId.matches("^[a-zA-Z0-9_]{1,10}$")){
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_ID_FORMAT);
        }
    }

    private void validateGender(Gender gender) {
        if (gender == null || gender.toString().isBlank()){
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.GENDER_CANNOT_BE_EMPTY);
        }
    }

    private void validateEmail(String email) {
        String emailRegex = "^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        if (email == null || !Pattern.matches(emailRegex, email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_EMAIL_FORMAT);
        }
    }

    private void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_BIRTH_FORMAT);
        }

        try {
            LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_BIRTH_FORMAT);
        }
    }

}
