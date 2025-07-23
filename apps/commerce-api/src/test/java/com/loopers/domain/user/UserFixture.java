package com.loopers.domain.user;

import com.loopers.domain.user.embeded.Grender;

public class UserFixture {
    public static final String USER_LOGIN_ID = "qwer1234";
    public static final String USER_EMAIL = "email@gmail.com";
    public static final String USER_BIRTH_DATE = "1990-01-01";
    public static final String USER_GENDER = Grender.GenderType.M.name();

    public static UserEntity createUser() {
        return UserEntity.register(USER_LOGIN_ID, USER_EMAIL, USER_BIRTH_DATE, USER_GENDER);
    }
    public static UserEntity createUser(String loginId, String email, String birth, String gender) {
        return UserEntity.register(loginId, email, birth, gender);
    }

    public static UserEntity createUserWithEmail(String email) {
        return createUser(USER_LOGIN_ID, email, USER_BIRTH_DATE, USER_GENDER);
    }

    public static UserEntity createUserWithGender(String gender) {
        return createUser(USER_LOGIN_ID, USER_EMAIL, USER_BIRTH_DATE, USER_GENDER);
    }

    public static UserEntity createUserWithBirthDate(String birthDate) {
        return createUser(USER_LOGIN_ID, USER_EMAIL, birthDate, USER_GENDER);
    }

    public static UserEntity createUserWithLoginId(String loginId) {
        return createUser(loginId, USER_EMAIL, USER_BIRTH_DATE, USER_GENDER);
    }
}
