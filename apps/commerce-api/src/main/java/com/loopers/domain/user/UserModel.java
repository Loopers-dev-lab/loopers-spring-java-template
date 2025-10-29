package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {

    private static final Pattern ID_RULE = Pattern.compile("^[A-Za-z0-9]{1,10}$");
    private static final Pattern EMAIL_RULE = Pattern.compile("^[A-Za-z0-9]+@[A-Za-z0-9]+\\.[A-Za-z0-9]+$");

    private String userId;
    private String email;
    private String birthDate;

    protected UserModel() {
    }

    protected UserModel(String userId, String email, String birthDate) {
        this.userId = userId;
        this.email = email;
        this.birthDate = birthDate;
    }

    public static UserModel create(String id, String email, String birthDate) {
        validateId(id);
        validateEmail(email);
        validateBirthDate(birthDate);

        return new UserModel(id, email, birthDate);
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
            throw new CoreException(ErrorType.BAD_REQUEST,"이메일은 비어있을 수 없습니다.");
        }
        if (!EMAIL_RULE.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST,"유효하지 않는 이메일 형식입니다.(xx@yy.zz)");
        }
    }

    private static void validateBirthDate(String birthDate) {
        if (Objects.isNull(birthDate)) {
            throw new CoreException(ErrorType.BAD_REQUEST,"생년월일은 비어있을 수 없습니다.");
        }

        try {
            LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않는 생년월일 형식입니다.(yyyy-MM-dd)");
        }
    }


}
