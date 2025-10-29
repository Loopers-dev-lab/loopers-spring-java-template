package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {

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
        if (Objects.isNull(id)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID은 비어있을 수 없습니다.");
        }

        if (Objects.isNull(email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }

        if (Objects.isNull(birthDate)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }

        return new UserModel(id, email, birthDate);
    }

}
