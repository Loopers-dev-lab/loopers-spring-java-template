package com.loopers.domain.user;

/**
 * 테스트용 고정 데이터 (Fixture) 클래스
 * 모든 User 관련 테스트에서 사용하는 공통 데이터를 관리
 */
public class UserTestFixture {

    // 기본 유효한 테스트 데이터
    public static final class ValidUser {
        public static final String USER_ID = "testuser";
        public static final String EMAIL = "test@example.com";
        public static final String BIRTH_DATE = "1990-01-01";
        public static final Point POINT = Point.of(0L);
    }

    // 유효하지 않은 테스트 데이터
    public static final class InvalidUser {
        public static final String USER_ID = "한글";
        public static final String EMAIL = "test";
        public static final String BIRTH_DATE = "2024.1.1";
        public static final Point POINT = Point.of(0L);
    }
}
