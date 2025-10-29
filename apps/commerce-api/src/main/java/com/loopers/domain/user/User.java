package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
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
            throw new CoreException(ErrorType.BAD_REQUEST, "아이디 형식이 올바르지 않습니다.");
        }
    }

    private void validateGender(String gender) {
        if (gender == null){
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 빈 값이 될 수 없습니다.");
        }
    }

    private void validateEmail(String email) {
        String emailRegex = "^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        if (email == null || !Pattern.matches(emailRegex, email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일이 xx@yy.zz 형식에 맞지 않습니다.");
        }
    }

    private void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 빈 값이 될 수 없습니다.");
        }

        try {
            LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 yyyy-MM-dd 형식에 맞지 않습니다.");
        }
    }

}
