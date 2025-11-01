package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class UserValidator {
    private static final String PATTERN_LOGIN_ID = "^[a-zA-Z0-9]{1,10}$";
    private static final String PATTERN_EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final String PATTERN_BIRTH = "^\\d{4}-\\d{2}-\\d{2}$";
    private static final String PATTERN_PASSWORD = "^(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$";

    public static void validateLoginId(String loginId) {
        if (loginId == null || !loginId.matches(PATTERN_LOGIN_ID)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Invalid loginId format: 로그인 ID는 영문 및 숫자 10자 이내로 입력해야 합니다.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !email.matches(PATTERN_EMAIL)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Invalid email format: 이메일은 xx@yy.zz 형식이어야 합니다.");
        }
    }

    public static void validateBirth(String birth) {
        if (birth == null || !birth.matches(PATTERN_BIRTH)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Invalid birth format: 생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || !password.matches(PATTERN_PASSWORD)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Invalid password format: 비밀번호는 8자 이상이어야 하며 특수문자를 포함해야 합니다.");
        }
    }

}
