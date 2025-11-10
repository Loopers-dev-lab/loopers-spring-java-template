package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {

    private String userId;
    private String email;
    private String birthdate;

    @Enumerated( EnumType.STRING )
    private Gender gender;
    private Integer point = 0;

    @Builder
    public User(String userId, String email, String birthdate, Gender gender) {

        validationUserId(userId);
        validationUserEmail(email);
        validationBirthdate(birthdate);

        this.userId = userId;
        this.email = email;
        this.birthdate = birthdate;
        this.gender = gender;
    }

    private void validationUserId(String userId) {
        if( userId == null || userId.isEmpty() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저 ID는 필수값입니다.");
        }

        if( !userId.matches("^[a-zA-Z0-9]{1,10}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저 ID 형식 오류");
        }
    }

    private void validationUserEmail(String email) {
        if( email == null || email.isEmpty() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수값입니다.");
        }

        if (!email.matches("^[\\w\\.]+@[\\w\\.]+\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식 오류");
        }
    }

    private static void validationBirthdate(String birthdate) {
        if( birthdate == null || birthdate.isEmpty() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수값입니다.");
        }

        if (!birthdate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일 형식 오류");
        }
    }

    protected void chargePoint(Integer point) {

        if( point <= 0 ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전할 포인트는 1 이상의 정수여야 합니다.");
        }

        this.point += point;
    }
}
