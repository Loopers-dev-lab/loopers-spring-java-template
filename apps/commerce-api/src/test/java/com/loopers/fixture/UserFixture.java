package com.loopers.fixture;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

public class UserFixture {

    // 기본 사용자
    public static User defaultUser() {
        return User.create("testuser01", "test@example.com", "1990-01-01", Gender.MALE);
    }

    // 커스텀 loginId
    public static User withLoginId(String loginId) {
        return User.create(loginId, loginId + "@example.com", "1990-01-01", Gender.MALE);
    }

    // 인덱스 기반 다수 생성용
    public static User indexed(int index) {
        String loginId = "user" + String.format("%03d", index);
        return User.create(loginId, loginId + "@example.com", "1990-01-01", Gender.MALE);
    }

    // 전체 커스텀
    public static User custom(String loginId, String email, String birthDate, Gender gender) {
        return User.create(loginId, email, birthDate, gender);
    }

    // 여성 사용자
    public static User female() {
        return User.create("femaleUser", "female@example.com", "1995-05-15", Gender.FEMALE);
    }

    // 남성 사용자
    public static User male() {
        return User.create("maleUser", "male@example.com", "1992-03-20", Gender.MALE);
    }
}
