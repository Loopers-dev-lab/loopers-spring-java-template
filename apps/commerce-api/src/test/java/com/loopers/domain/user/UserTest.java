package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {
    @DisplayName("User 단위 테스트")
    @Nested
    class Create {
        @DisplayName("ID 가 영문 및 숫자 10자 이내 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenInvalidUserIdFormat() {
            // given
            String invalidUserId = "invalid_id_123"; // 10자 초과 + 특수문자 포함
            String email = "valid@loopers.com";
            String birth = "1994-12-05";
            String gender = "MALE";

            // when & then
            assertThrows(CoreException.class, () -> new User(invalidUserId, email, birth, gender));
        }

        @DisplayName("이메일이 `xx@yy.zz` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenInvalidEmailFormat() {
            // given
            String userId = "yh45g";
            String invalidEmail = "invalid-email-format"; // '@' 없음
            String birth = "1994-12-05";
            String gender = "MALE";

            // when & then
            assertThrows(CoreException.class, () -> new User(userId, invalidEmail, birth, gender));
        }

        @DisplayName("생년월일이 `yyyy-MM-dd` 형식에 맞지 않으면, User 객체 생성에 실패한다.")
        @Test
        void throwsException_whenInvalidBirthFormat() {
            // given
            String userId = "yh45g";
            String email = "valid@loopers.com";
            String invalidBirth = "19941205"; // 형식 오류: 하이픈 없음
            String gender = "MALE";

            // when & then
            assertThrows(CoreException.class, () -> new User(userId, email, invalidBirth, gender));
        }
    }
}
