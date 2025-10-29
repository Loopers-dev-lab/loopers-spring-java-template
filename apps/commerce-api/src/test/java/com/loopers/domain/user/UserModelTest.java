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

        @Test
        void ID_누락시_생성에_실패한다() {
            assertThatThrownBy(() -> UserModel.create(null, VALID_EMAIL, VALID_BIRTH_DATE))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("ID은 비어있을 수 없습니다.");
        }

        @Test
        void 이메일_누락시_생성에_실패한다() {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, null, VALID_BIRTH_DATE))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("이메일은 비어있을 수 없습니다.");
        }

        @Test
        void 생년월일_누락시_생성에_실패한다() {
            assertThatThrownBy(() -> UserModel.create(VALID_ID, VALID_EMAIL, null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("생년월일은 비어있을 수 없습니다.");
        }

        @Test
        void 필수값이_있으면_생성에_성공한다() {
            assertDoesNotThrow(() ->
                    UserModel.create(VALID_ID, VALID_EMAIL, VALID_BIRTH_DATE)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab_", "abc-123", "한글"})
        void ID_영문_및_숫자_이외_값이_있으면_User_객체_생성에_실패한다(String id) {
            assertThatThrownBy(() -> UserModel.create(id, VALID_EMAIL, VALID_BIRTH_DATE))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 ID 형식입니다.(영문 및 숫자 10자이내)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abcdefghijk", "12345678901"})
        void ID가_10자를_초과하면_User_객체_생성에_실패한다(String id) {
            assertThatThrownBy(() -> UserModel.create(id, VALID_EMAIL, VALID_BIRTH_DATE))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("유효하지 않는 ID 형식입니다.(영문 및 숫자 10자이내)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "123", "abc123"})
        void ID_영문_및_숫자_10자_이내_형식에_맞으면_User_객체_생성에_성공한다(String id) {
            assertDoesNotThrow(() ->
                    UserModel.create(id, VALID_EMAIL, VALID_BIRTH_DATE)
            );
        }
    }
}
