package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class UserModel extends BaseEntity {

    private String userId;
    private String email;
    private String birthDate;

    protected UserModel() {}

    public UserModel(String userId, String email, String birthDate) {

        if ( userId == null || userId.isBlank() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "UserId는 비어있을 수 없습니다.");
        }
        if ( !userId.matches("^[a-zA-Z0-9_-]{1,10}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID 가 `영문 및 숫자 10자 이내` 형식에 맞아야 합니다.");
        }

        if ( email == null || email.isBlank() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if ( !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일이 `xx@yy.zz` 형식에 맞아야 합니다.");
        }

        if ( birthDate == null || birthDate.isBlank() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
        if ( !birthDate.matches("^\\d{4}-\\d{2}-\\d{2}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 `yyyy-MM-dd` 형식에 맞아야 합니다.");
        }

        this.userId = userId;
        this.email = email;
        this.birthDate = birthDate;
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

}
