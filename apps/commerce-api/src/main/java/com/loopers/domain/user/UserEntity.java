package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private String birth;

    private String password;

    private BigDecimal pointBalance = BigDecimal.ZERO;

    public UserEntity(String loginId, String email, Gender gender, String birth, String password) {
        UserValidator.validateLoginId(loginId);
        UserValidator.validateEmail(email);
        UserValidator.validateBirth(birth);
        UserValidator.validatePassword(password);

        this.loginId = loginId;
        this.email = email;
        this.gender = gender;
        this.birth = birth;
        this.password = password;
    }

    public void addPoints(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 반드시 0보다 커야 합니다.");
        }
        this.pointBalance = this.pointBalance.add(amount);
    }

    public void deductPoints(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 반드시 0보다 커야 합니다.");
        }
        if (this.pointBalance.compareTo(amount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액이 부족합니다.");
        }
        this.pointBalance = this.pointBalance.subtract(amount);
    }

}
