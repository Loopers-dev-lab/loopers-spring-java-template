package com.loopers.fixtures;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;

import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserDomainCreateRequest;
import com.loopers.domain.user.UserEntity;
import com.loopers.interfaces.api.user.UserV1Dtos;

/**
 * User 관련 테스트 픽스처 클래스
 * 테스트에서 자주 사용되는 User 객체 생성 메서드들을 제공합니다.
 *
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public class UserTestFixture {

    // 기본 유효한 데이터
    public static final String DEFAULT_USERNAME = "testuser";
    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_BIRTHDATE = "1990-01-01";
    public static final Gender DEFAULT_GENDER = Gender.MALE;

    // 유효하지 않은 데이터
    public static final String INVALID_USERNAME_TOO_LONG = "testuser123"; // 11자
    public static final String INVALID_USERNAME_SPECIAL_CHAR = "test@user";
    public static final String INVALID_EMAIL = "invalid-email";
    public static final String INVALID_BIRTHDATE = "1990/01/01";

    /**
     * 기본값으로 UserDomainCreateRequest 생성
     */
    public static UserDomainCreateRequest createDefaultUserDomainRequest() {
        return new UserDomainCreateRequest(DEFAULT_USERNAME, DEFAULT_EMAIL, DEFAULT_BIRTHDATE, DEFAULT_GENDER);
    }

    /**
     * 커스텀 UserDomainCreateRequest 생성
     */
    public static UserDomainCreateRequest createUserDomainRequest(String username, String email, String birthdate,
                                                                  Gender gender) {
        return new UserDomainCreateRequest(username, email, birthdate, gender);
    }

    /**
     * 기본값으로 UserV1Dtos.UserRegisterRequest 생성 (API 계층용)
     */
    public static UserV1Dtos.UserRegisterRequest createDefaultApiRequest() {
        return new UserV1Dtos.UserRegisterRequest(DEFAULT_USERNAME, DEFAULT_EMAIL, DEFAULT_BIRTHDATE, DEFAULT_GENDER);
    }

    /**
     * 커스텀 UserV1Dtos.UserRegisterRequest 생성 (API 계층용)
     */
    public static UserV1Dtos.UserRegisterRequest createApiRequest(String username, String email, String birthdate,
                                                                  Gender gender) {
        return new UserV1Dtos.UserRegisterRequest(username, email, birthdate, gender);
    }

    /**
     * 기본값으로 UserEntity 생성
     */
    public static UserEntity createDefaultUserEntity() {
        UserDomainCreateRequest request = createDefaultUserDomainRequest();
        return UserEntity.createUserEntity(request);
    }

    /**
     * 커스텀 UserEntity 생성
     */
    public static UserEntity createUserEntity(String username, String email, String birthdate, Gender gender) {
        UserDomainCreateRequest request = createUserDomainRequest(username, email, birthdate, gender);
        return UserEntity.createUserEntity(request);
    }

    /**
     * 기본값으로 UserRegisterCommand 생성
     */
    public static UserRegisterCommand createDefaultUserCommand() {
        return UserRegisterCommand.of(DEFAULT_USERNAME, DEFAULT_EMAIL, DEFAULT_BIRTHDATE, DEFAULT_GENDER);
    }

    /**
     * 커스텀 UserRegisterCommand 생성
     */
    public static UserRegisterCommand createUserCommand(String username, String email, String birthdate,
                                                        Gender gender) {
        return UserRegisterCommand.of(username, email, birthdate, gender);
    }

    /**
     * UserEntity 검증 헬퍼 메서드
     */
    public static void assertUserEntity(UserEntity actual, UserDomainCreateRequest expected) {
        Assertions.assertThat(actual.getId()).isNotNull();
        Assertions.assertThat(actual.getUsername()).isEqualTo(expected.username());
        Assertions.assertThat(actual.getEmail()).isEqualTo(expected.email());
        Assertions.assertThat(actual.getBirthdate()).isEqualTo(LocalDate.parse(expected.birthdate()));
        Assertions.assertThat(actual.getGender()).isEqualTo(expected.gender());
    }

    /**
     * 저장된 UserEntity 검증 헬퍼 메서드 (DB 저장 후 검증용)
     */
    public static void assertUserEntityPersisted(UserEntity actual, UserDomainCreateRequest expected) {
        Assertions.assertThat(actual.getId()).isNotNull();
        Assertions.assertThat(actual.getUsername()).isEqualTo(expected.username());
        Assertions.assertThat(actual.getEmail()).isEqualTo(expected.email());
        Assertions.assertThat(actual.getBirthdate()).isEqualTo(LocalDate.parse(expected.birthdate()));
        Assertions.assertThat(actual.getGender()).isEqualTo(expected.gender());
        Assertions.assertThat(actual.getCreatedAt()).isNotNull();
        Assertions.assertThat(actual.getDeletedAt()).isNull();
    }

    /**
     * User 생성 실패 검증 헬퍼 메서드
     */
    public static void assertUserCreationFails(String username, String email, String birthdate, Gender gender,
                                               String expectedMessage) {
        UserDomainCreateRequest request = createUserDomainRequest(username, email, birthdate, gender);

        Assertions.assertThatThrownBy(() -> UserEntity.createUserEntity(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    /**
     * 무효한 사용자명 테스트 데이터
     */
    public static class InvalidUsername {
        public static final String TOO_LONG = INVALID_USERNAME_TOO_LONG;
        public static final String SPECIAL_CHAR = INVALID_USERNAME_SPECIAL_CHAR;
        public static final String EXPECTED_MESSAGE = "사용자명은 영문 및 숫자 10자 이내여야 합니다.";
    }

    /**
     * 무효한 이메일 테스트 데이터
     */
    public static class InvalidEmail {
        public static final String INVALID_FORMAT = INVALID_EMAIL;
        public static final String EXPECTED_MESSAGE = "올바른 형식의 이메일 주소여야 합니다.";
    }

    /**
     * 무효한 생년월일 테스트 데이터
     */
    public static class InvalidBirthdate {
        public static final String INVALID_FORMAT = INVALID_BIRTHDATE;
        public static final String NULL_MESSAGE = "생년월일은 필수 입력값입니다.";
        public static final String FORMAT_MESSAGE = "올바른 날짜 형식이어야 합니다.";
    }

    /**
     * 무효한 성별 테스트 데이터
     */
    public static class InvalidGender {
        public static final String EXPECTED_MESSAGE = "성별은 필수 입력값입니다.";
    }

    /**
     * 사용자의 포인트가 0인지 검증하는 헬퍼 메서드
     */
    public static void assertUserPointIsZero(UserEntity user) {
        Assertions.assertThat(user.getPointAmount()).isEqualByComparingTo(java.math.BigDecimal.ZERO.setScale(2));
    }

    /**
     * 사용자의 포인트 금액 검증 헬퍼 메서드
     */
    public static void assertUserPointAmount(UserEntity user, java.math.BigDecimal expectedAmount) {
        Assertions.assertThat(user.getPointAmount()).isEqualByComparingTo(expectedAmount);
    }

    /**
     * 포인트 충전 실패 검증 헬퍼 메서드
     */
    public static void assertChargePointFails(UserEntity user, java.math.BigDecimal amount, String expectedMessage) {
        Assertions.assertThatThrownBy(() -> user.chargePoint(amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
