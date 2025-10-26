package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserEmail 테스트")
class UserEmailTest {

    @DisplayName("UserEmail을 생성할 때")
    @Nested
    class Create {

        @DisplayName("xx@yy.zz 형식이면 정상 생성")
        @Test
        void shouldCreate_whenValidFormat() {
            String validEmail = "user@example.com";

            UserEmail result = new UserEmail(validEmail);

            assertThat(result)
                .extracting("email")
                .isEqualTo(validEmail);
        }

        @DisplayName("null 또는 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowBadRequest_whenNullOrEmpty(String invalidEmail) {
            assertThatThrownBy(() -> new UserEmail(invalidEmail))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("비어있을 수 없습니다");
        }

        @DisplayName("xx@yy.zz 형식이 아니면 BAD_REQUEST 예외가 발생한다")
        @ParameterizedTest
        @ValueSource(strings = {
            "userexample.com",       // @ 없음
            "@example.com",          // 로컬파트 없음
            "user@",                 // 도메인 없음
            "user@example",          // TLD 없음
            "user example@test.com", // 공백
            "user@exam ple.com",     // 도메인 공백
            "user@@example.com"      // @ 중복
        })
        void shouldThrowBadRequest_whenInvalidFormat(String invalidEmail) {
            assertThatThrownBy(() -> new UserEmail(invalidEmail))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("이메일 형식이 올바르지 않습니다");
        }

    }
}
