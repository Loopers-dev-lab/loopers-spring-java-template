package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UserModelTest {
    @DisplayName("User 모델을 생성할 때, ")
    @Nested
    class Create {
        private static final String VALID_ID = "abc";
        private static final String VALID_EMAIL = "xx@yy.zz";
        private static final String VALID_BIRTH_DATE = "2000-01-01";
        private static final Gender VALID_GENDER = Gender.FEMALE;

        @Test
        void ID_누락시_생성에_실패한다() {
            assertThatThrownBy(() -> UserModel.create(null, VALID_EMAIL, VALID_BIRTH_DATE, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("ID은 비어있을 수 없습니다.");
        }

        @Test
        void 이메일_누락시_생성에_실패한다() {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, null, VALID_BIRTH_DATE, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("이메일은 비어있을 수 없습니다.");
        }

        @Test
        void 생년월일_누락시_생성에_실패한다() {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, VALID_EMAIL, null, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("생년월일은 비어있을 수 없습니다.");
        }

        @Test
        void 필수값이_있으면_생성에_성공한다() {
            assertDoesNotThrow(() ->
                    UserModel.create(VALID_ID, VALID_EMAIL, VALID_BIRTH_DATE, VALID_GENDER)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab_", "abc-123", "한글"})
        void ID_영문_및_숫자_이외_값이_있으면_User_객체_생성에_실패한다(String id) {
            assertThatThrownBy(() -> UserModel.create(id, VALID_EMAIL, VALID_BIRTH_DATE, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 ID 형식입니다.(영문 및 숫자 10자이내)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abcdefghijk", "12345678901"})
        void ID가_10자를_초과하면_User_객체_생성에_실패한다(String id) {
            assertThatThrownBy(() -> UserModel.create(id, VALID_EMAIL, VALID_BIRTH_DATE, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 ID 형식입니다.(영문 및 숫자 10자이내)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "123", "abc123"})
        void ID_영문_및_숫자_10자_이내_형식에_맞으면_User_객체_생성에_성공한다(String id) {
            assertDoesNotThrow(() ->
                    UserModel.create(id, VALID_EMAIL, VALID_BIRTH_DATE, VALID_GENDER)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"a@b", "ab@cd.", "@cd.com", "ab@.com"})
        void 이메일_형식에_맞지_않으면_User_객체_생성에_실패한다(String email) {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, email, VALID_BIRTH_DATE, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 이메일 형식입니다.(xx@yy.zz)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab@cd.com", "a123@b.co.kr"})
        void 이메일_형식에_맞으면_User_객체_생성에_성공한다(String email) {
            assertDoesNotThrow(() ->
                    UserModel.create(VALID_ID, email, VALID_BIRTH_DATE, VALID_GENDER)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"00-01-01", "2000/01/01", "2000.01.01"})
        void 생년월일_형식에_맞지_않으면_User_객체_생성에_실패한다(String birthDate) {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, VALID_EMAIL, birthDate, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 생년월일 형식입니다.(yyyy-MM-dd)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"1234-56-78", "abcd-ef-gh"})
        void 날짜_형식에_맞지_않으면_User_객체_생성에_실패한다(String birthDate) {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, VALID_EMAIL, birthDate, VALID_GENDER))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 생년월일 형식입니다.(yyyy-MM-dd)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2000-01-01", "1900-12-31"})
        void 생년월일_형식에_맞으면_User_객체_생성에_성공한다(String birthDate) {
            assertDoesNotThrow(() ->
                    UserModel.create(VALID_ID, VALID_EMAIL, birthDate, VALID_GENDER)
            );
        }
    }
}
