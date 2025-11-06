package com.loopers.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.interfaces.api.point.PointV1Dtos;

/**
 * Point 테스트를 위한 공통 픽스처 클래스
 *
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public class PointTestFixture {

    // 기본 포인트 테스트 데이터
    public static final BigDecimal DEFAULT_POINT_AMOUNT = BigDecimal.ZERO.setScale(2);
    public static final BigDecimal CHARGE_AMOUNT_1000 = new BigDecimal("1000");
    public static final BigDecimal CHARGE_AMOUNT_1000_SCALED = new BigDecimal("1000.00");
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    public static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-100");

    public static final String NONEXISTENT_USERNAME = "nonexistentuser";

    // HTTP 헤더 관련 상수
    public static final String USER_ID_HEADER = "X-USER-ID";

    // 에러 메시지
    public static final String ERROR_INVALID_CHARGE_AMOUNT = "충전 금액은 0보다 커야 합니다.";
    public static final String ERROR_USER_NOT_FOUND = "존재하지 않는 사용자입니다.";

    /**
     * 기본 UserRegisterCommand 생성
     */
    public static UserRegisterCommand createDefaultUserRegisterCommand() {
        return new UserRegisterCommand(
                "testuser",
                "test@example.com",
                "1990-01-01",
                Gender.MALE
        );
    }

    /**
     * 커스텀 UserRegisterCommand 생성
     */
    public static UserRegisterCommand createUserRegisterCommand(String username, String email, String birthdate,
                                                                Gender gender) {
        return new UserRegisterCommand(username, email, birthdate, gender);
    }

    /**
     * UserRegisterCommand.of() 메서드를 사용한 생성
     */
    public static UserRegisterCommand createUserRegisterCommandOf(String username, String email, String birthdate,
                                                                  Gender gender) {
        return UserRegisterCommand.of(username, email, birthdate, gender);
    }

    /**
     * X-USER-ID 헤더가 포함된 HttpHeaders 생성
     */
    public static HttpHeaders createUserIdHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(USER_ID_HEADER, username);
        return headers;
    }

    /**
     * 기본 사용자명으로 X-USER-ID 헤더 생성
     */
    public static HttpHeaders createDefaultUserIdHeaders() {
        return createUserIdHeaders("testuser");
    }

    /**
     * 사용자의 포인트 금액 검증 헬퍼 메서드
     */
    public static void assertUserPointAmount(UserEntity user, BigDecimal expectedAmount) {
        assertThat(user).isNotNull();
        assertThat(user.getPointAmount()).isEqualByComparingTo(expectedAmount);
    }

    /**
     * 사용자의 포인트가 0인지 검증하는 헬퍼 메서드
     */
    public static void assertUserPointIsZero(UserEntity user) {
        assertThat(user).isNotNull();
        assertThat(user.getPointAmount()).isEqualByComparingTo(DEFAULT_POINT_AMOUNT);
    }

    /**
     * PointChargeRequest 생성
     */
    public static PointV1Dtos.PointChargeRequest createChargeRequest(BigDecimal amount) {
        return new PointV1Dtos.PointChargeRequest(amount);
    }

    /**
     * 기본 충전 요청 생성 (1000원)
     */
    public static PointV1Dtos.PointChargeRequest createDefaultChargeRequest() {
        return createChargeRequest(CHARGE_AMOUNT_1000);
    }

    /**
     * X-USER-ID 헤더와 Content-Type이 포함된 HttpHeaders 생성
     */
    public static HttpHeaders createChargeHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(USER_ID_HEADER, username);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 테스트용 사용자 데이터 클래스
     */
    public static class TestUser {
        public static final String USERNAME = "testuser";
        public static final String EMAIL = "dvum0045@gmail.com";
        public static final String BIRTHDATE = "1990-01-01";
        public static final Gender GENDER = Gender.FEMALE;

        public static UserRegisterCommand createCommand() {
            return createUserRegisterCommand(USERNAME, EMAIL, BIRTHDATE, GENDER);
        }

        public static UserRegisterCommand createCommandOf() {
            return createUserRegisterCommandOf(USERNAME, EMAIL, BIRTHDATE, GENDER);
        }
    }
}
