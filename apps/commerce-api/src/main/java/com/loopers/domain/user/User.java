package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    private static final Pattern ID_RULE = Pattern.compile("^[A-Za-z0-9]{1,10}$");
    private static final Pattern EMAIL_RULE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");

    private String userId;
    private String email;
    private String birthDate;
    @Enumerated(EnumType.STRING)
    private Gender gender;

    protected User() {
    }

    protected User(String userId, String email, String birthDate, Gender gender) {
        this.userId = userId;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public static User create(String id, String email, String birthDate, Gender gender) {
        validateId(id);
        validateEmail(email);
        validateBirthDate(birthDate);
        validateGender(gender);

        return new User(id, email, birthDate, gender);
    }

    private static void validateId(String id) {
        if (Objects.isNull(id)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID은 비어있을 수 없습니다.");
        }

        if (!ID_RULE.matcher(id).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않는 ID 형식입니다.(영문 및 숫자 10자이내)");
        }
    }

    private static void validateEmail(String email) {
        if (Objects.isNull(email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if (!EMAIL_RULE.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않는 이메일 형식입니다.(xx@yy.zz)");
        }
    }

    private static void validateBirthDate(String birthDate) {
        if (Objects.isNull(birthDate)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }

        try {
            LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않는 생년월일 형식입니다.(yyyy-MM-dd)");
        }
    }

    private static void validateGender(Gender gender) {
        if (Objects.isNull(gender)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 비어있을 수 없습니다.");
        }
    }


    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public Gender getGender() {
        return gender;
    }
}
