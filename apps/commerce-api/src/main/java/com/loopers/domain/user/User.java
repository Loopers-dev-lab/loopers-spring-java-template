package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.error.ErrorMessages;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
public class User {

    @Id
    private String id;
    private String gender;
    private String email;
    private String birthDate;

    @Builder
    public User(String id, String gender, String email, String birthDate) {
        validateId(id);
        validateGender(gender);
        validateEmail(email);
        validateBirthDate(birthDate);

        this.id = id;
        this.gender = gender;
        this.email = email;
        this.birthDate = birthDate;
    }

    private void validateId(String id) {
        if (id == null || !id.matches("^[a-zA-Z0-9_]{1,10}$")){
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_ID_FORMAT);
        }
    }

    private void validateGender(String gender) {
        if (gender == null || gender.isEmpty()){
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
