package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class UserModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String email;
    private String birthdate;
    private String gender;

    @Builder
    public UserModel(String userId, String email, String birthdate, String gender) {

        validUserInfo(userId, email, birthdate);

        this.userId = userId;
        this.email = email;
        this.birthdate = birthdate;
        this.gender = gender;
    }

    public UserModel() {
    }

    private static void validUserInfo(String userId, String email, String birthdate) {
        if( !userId.matches("^[a-zA-Z0-9]{1,10}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저 ID 형식 오류");
        }

        if (!email.matches("^[\\w\\.]+@[\\w\\.]+\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식 오류");
        }

        if (!birthdate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일 형식 오류");
        }
    }
}
