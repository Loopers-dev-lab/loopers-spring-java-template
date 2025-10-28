package com.loopers.core.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserEmail")
class UserEmailTest {

    @Nested
    @DisplayName("create() 메서드")
    class CreateMethod {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @Test
            @DisplayName("UserEmail 객체를 생성한다")
            void 객체_생성() {
                // given
                String validEmail = "user@example.com";

                // when
                UserEmail email = UserEmail.create(validEmail);

                // then
                assertThat(email.value()).isEqualTo(validEmail);
            }
        }

        @Nested
        @DisplayName("value가 null인 경우")
        class value가_null인_경우 {

            @Test
            @DisplayName("NullPointerException을 발생시킨다")
            void 예외_발생() {
                // given
                String email = null;

                // when & then
                assertThatThrownBy(() -> UserEmail.create(email))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("사용자의 이메일는(은) Null이 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("value가 유효한 이메일 형식이 아닌 경우")
        class value가_유효한_이메일_형식이_아닌_경우 {

            static Stream<String> invalidEmails() {
                return Stream.of(
                        "invalid.email",              // @ 없음
                        "user@",                      // 도메인 없음
                        "@example.com",               // 로컬 부분 없음
                        "user name@example.com",      // 공백 포함
                        "user@example",               // TLD 없음
                        "user@.com",                  // 도메인명 없음
                        "user@@example.com",          // @ 중복
                        "user@exam ple.com"           // 도메인에 공백
                );
            }

            @ParameterizedTest
            @MethodSource("invalidEmails")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(String email) {
                // when & then
                assertThatThrownBy(() -> UserEmail.create(email))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("유효하지 않은 이메일 형식입니다.");
            }
        }
    }
}
