package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    }
}
