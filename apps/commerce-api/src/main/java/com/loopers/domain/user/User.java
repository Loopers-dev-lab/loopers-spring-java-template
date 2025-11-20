package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String birthdate;
    private String email;

    /**
     * @deprecated 포인트 관리는 {@link com.loopers.domain.point.Point}를 사용하세요.
     * 이 필드는 하위 호환성을 위해 유지되며, 향후 버전에서 제거될 예정입니다.
     */
    @Deprecated
    private BigDecimal point;

    @Builder
    private User(String userId, Gender gender, String birthdate, String email, BigDecimal point) {
        validateUserId(userId);
        validateGender(gender);
        validateEmail(email);
        validateBirthDate(birthdate);

        this.userId = userId;
        this.gender = gender;
        this.birthdate = birthdate;
        this.email = email;
        this.point = point != null ? point : BigDecimal.ZERO;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User ID는 필수입니다.");
        }
        if (!userId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "User ID는 영문 및 숫자 10자 이내여야 합니다.");
        }
    }

    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수입니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        }

        // 간단한 이메일 패턴: xx@yy.zz
        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "이메일은 xx@yy.zz 형식이어야 합니다.");
        }
    }

    private void validateBirthDate(String birthdate) {
        if (birthdate == null || birthdate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수입니다.");
        }
        if (!birthdate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    /**
     * @deprecated 포인트 충전은 {@link com.loopers.domain.point.PointService#chargePoint(String, BigDecimal)}를 사용하세요.
     * 이 메서드는 하위 호환성을 위해 유지되며, 향후 버전에서 제거될 예정입니다.
     */
    @Deprecated
    public void chargePoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "충전 금액은 0보다 커야 합니다.");
        }
        this.point = this.point.add(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(getUserId(), user.getUserId())
            && getGender() == user.getGender() && Objects.equals(getBirthdate(),
            user.getBirthdate()) && Objects.equals(getEmail(), user.getEmail())
            && Objects.equals(getPoint(), user.getPoint());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getGender(), getBirthdate(), getEmail(), getPoint());
    }
}
