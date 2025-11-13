package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 사용자 도메인 엔티티.
 * <p>
 * 사용자의 기본 정보(ID, 이메일, 생년월일, 성별)를 관리하며,
 * 각 필드에 대한 유효성 검증을 수행합니다.
 * </p>
 *
 * <h3>검증 규칙</h3>
 * <ul>
 *   <li>userId: 영문 및 숫자 조합, 최대 10자</li>
 *   <li>email: 유효한 이메일 형식</li>
 *   <li>birthDate: yyyy-MM-dd 형식</li>
 * </ul>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {
    @Column(name = "user_id", unique = true, nullable = false, length = 10)
    private String userId;

    private String email;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Embedded
    private Point point;

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");
    /**
     * 사용자 ID의 유효성을 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @throws CoreException userId가 null, 공백이거나 형식에 맞지 않을 경우
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 필수입니다.");
        }
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자 10자 이내여야 합니다.");
        }
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    /**
     * 이메일의 유효성을 검증합니다.
     *
     * @param email 검증할 이메일 주소
     * @throws CoreException email이 null, 공백이거나 형식에 맞지 않을 경우
     */
    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 생년월일의 유효성을 검증합니다.
     *
     * @param birthDate 검증할 생년월일 문자열
     * @throws CoreException birthDate가 null, 공백이거나 yyyy-MM-dd 형식이 아닐 경우
     */
    private static void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수입니다.");
        }
        try {
            LocalDate.parse(birthDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }
    /**
     * 사용자를 생성합니다.
     *
     * @param userId 사용자 ID (영문 및 숫자, 최대 10자)
     * @param email 이메일 주소
     * @param birthDateStr 생년월일 (yyyy-MM-dd 형식)
     * @param gender 성별
     * @throws CoreException userId, email, birthDate가 유효하지 않을 경우
     */
    public User (String userId, String email, String birthDateStr, Gender gender, Point point) {
        validateUserId(userId);
        validateEmail(email);
        validateBirthDate(birthDateStr);

        this.userId = userId;
        this.email = email;
        this.birthDate = LocalDate.parse(birthDateStr);
        this.gender = gender;
        this.point = point;
    }
    /**
     * User 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDate 생년월일 문자열
     * @param gender 성별
     * @return 생성된 User 인스턴스
     * @throws CoreException 유효성 검증 실패 시
     */
    public static User of(String userId, String email, String birthDate, Gender gender, Point point) {
        return new User(userId, email, birthDate, gender, point);
    }

    /**
     * 포인트를 반환합니다.
     *
     * @return 포인트 Value Object
     */
    public Point getPoint() {
        return this.point;
    }

    /**
     * 포인트를 받습니다 (충전/환불).
     *
     * @param point 받을 포인트
     * @throws CoreException point가 null일 경우
     */
    public void receivePoint(Point point) {
        this.point = this.point.add(point);
    }

    /**
     * 포인트를 차감합니다.
     * 포인트는 감소만 가능하며 음수가 되지 않도록 도메인 레벨에서 검증합니다.
     *
     * @param point 차감할 포인트
     * @throws CoreException point가 null이거나 잔액이 부족할 경우
     */
    public void deductPoint(Point point) {
        this.point = this.point.subtract(point);
    }

}
