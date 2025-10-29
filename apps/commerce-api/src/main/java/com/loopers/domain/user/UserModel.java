package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {

    private static final Pattern ID_RULE = Pattern.compile("^[A-Za-z0-9]{1,10}$");

    private String id;
    private String email;
    private String birthDate;

    protected UserModel() {
    }

    protected UserModel(String id, String email, String birthDate) {
        this.id = id;
        this.email = email;
        this.birthDate = birthDate;
    }

    public static UserModel create(String id, String email, String birthDate) {
        validateId(id);

        if (Objects.isNull(email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }

        if (Objects.isNull(birthDate)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }

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

}
