package com.loopers.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("회원 도메인 단위 테스트")
class UserTest {
    @DisplayName("회원 가입")
    @Nested
    class Create {
        @Nested
        class ValidId {
            @DisplayName("ID가 '영문 및 숫자 10자 이내' 형식에 맞지 않으면, User객체 생성에 실패한다.")
            @Test
            void should_fail_to_create_user_when_id_is_invalid() {
                // given
                String invalidUserId1 = "abcde123456";
                String invalidUserId2 = "ㅋㅋㅋ";
                String invalidUserId3 = "abc@#";

                // when & then
                assertThatThrownBy(() -> User.create(invalidUserId1, "xx@yy.zz", "1999-01-31", "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID는 영문 및 숫자 10자 이내여야 합니다.");

                assertThatThrownBy(() -> User.create(invalidUserId2, "xx@yy.zz", "1999-01-31", "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID는 영문 및 숫자 10자 이내여야 합니다.");

                assertThatThrownBy(() -> User.create(invalidUserId3, "xx@yy.zz", "1999-01-31", "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("ID는 영문 및 숫자 10자 이내여야 합니다.");
            }
        }

        @Nested
        class ValidEmail {
            @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다.")
            @Test
            void should_fail_to_create_user_when_email_is_invalid() {
                // given
                String invalidEmail1 = "email";
                String invalidEmail2 = "@example.com";
                String invalidEmail3 = "test@";

                // when & then
                assertThatThrownBy(() -> User.create("testuser1", invalidEmail1, "1999-01-31", "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이메일은 xx@yy.zz 형식이어야 합니다.");

                assertThatThrownBy(() -> User.create("testuser1", invalidEmail2, "1999-01-31", "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이메일은 xx@yy.zz 형식이어야 합니다.");

                assertThatThrownBy(() -> User.create("testuser1", invalidEmail3, "1999-01-31", "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이메일은 xx@yy.zz 형식이어야 합니다.");
            }
        }

        @Nested
        class ValidBirthDate {
            @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다.")
            @Test
            void should_fail_to_create_user_when_birth_date_is_invalid() {
                // given
                String invalidBirthDate1 = "1990-1-31";
                String invalidBirthDate2 = "01-01-1990";
                String invalidBirthDate3 = "19900101";

                // when & then
                assertThatThrownBy(() -> User.create("testuser1", "xx@yy.zz", invalidBirthDate1, "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("생년월일은 yyyy-MM-dd 형식이어야 합니다.");

                assertThatThrownBy(() -> User.create("testuser1", "xx@yy.zz", invalidBirthDate2, "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("생년월일은 yyyy-MM-dd 형식이어야 합니다.");

                assertThatThrownBy(() -> User.create("testuser1", "xx@yy.zz", invalidBirthDate3, "MALE"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("생년월일은 yyyy-MM-dd 형식이어야 합니다.");
            }
        }

        @Nested
        class ValidGender {
            @DisplayName("성별이 MALE 또는 FEMALE이 아니면, User 객체 생성에 실패한다.")
            @Test
            void should_fail_to_create_user_when_gender_is_invalid() {
                // given
                String invalidGender1 = "UNKNOWN";
                String invalidGender2 = "male";
                String invalidGender3 = "";

                // when & then
                assertThatThrownBy(() -> User.create("testuser1", "xx@yy.zz", "1999-01-31", invalidGender1))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("성별은 MALE 또는 FEMALE이어야 합니다.");

                assertThatThrownBy(() -> User.create("testuser1", "xx@yy.zz", "1999-01-31", invalidGender2))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("성별은 MALE 또는 FEMALE이어야 합니다.");

                assertThatThrownBy(() -> User.create("testuser1", "xx@yy.zz", "1999-01-31", invalidGender3))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("성별은 MALE 또는 FEMALE이어야 합니다.");
            }
        }
    }
}
