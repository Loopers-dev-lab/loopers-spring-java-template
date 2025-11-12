package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 10)
    private String userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "birthdate", nullable = false)
    private String birthdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "point", nullable = false, columnDefinition = "decimal(15,2) default 0")
    private BigDecimal point = BigDecimal.ZERO;

    @Builder
    protected User(String userId, String email, String birthdate, Gender gender) {
        validateUserId(userId);
        validateUserEmail(email);
        validateBirthdate(birthdate);
        validateGender(gender);

        this.userId = userId;
        this.email = email;
        this.birthdate = birthdate;
        this.gender = gender;
    }

    public static User createUser(String userId, String email, String birthdate, Gender gender) {
        return User.builder()
                .userId(userId)
                .email(email)
                .birthdate(birthdate)
                .gender(gender)
                .build();
    }

    private static void validateUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저 ID는 필수값입니다");
        }

        if (!userId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저 ID는 영문자와 숫자로만 구성된 1-10자여야 합니다");
        }
    }

    private static void validateUserEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수값입니다");
        }

        if (!email.matches("^[\\w\\.]+@[\\w\\.]+\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다");
        }
    }

    private static void validateBirthdate(String birthdate) {
        if (birthdate == null || birthdate.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수값입니다");
        }

        if (!birthdate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 YYYY-MM-DD 형식이어야 합니다");
        }
    }

    private static void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수값입니다");
        }
    }

    public void chargePoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전할 포인트는 양수여야 합니다");
        }

        this.point = this.point.add(amount);
    }

    public void usePoint(BigDecimal amount) {
        validateAmount(amount);

        this.point = this.point.subtract(amount);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 포인트는 양수여야 합니다");
        }

        if (this.point.compareTo(amount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다. 현재 포인트: " + this.point);
        }
    }
}
