package com.loopers.core.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserIdentifier")
class UserIdentifierTest {

    @Nested
    @DisplayName("create() 메서드")
    class CreateMethod {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @Test
            @DisplayName("UserIdentifier 객체를 생성한다")
            void 객체_생성() {
                // given
                String validId = "user123";

                // when
                UserIdentifier identifier = UserIdentifier.create(validId);

                // then
                assertThat(identifier.value()).isEqualTo(validId);
            }
        }

        @Nested
        @DisplayName("value가 null인 경우")
        class value가_null인_경우 {

            @Test
            @DisplayName("NullPointerException을 발생시킨다")
            void 예외_발생() {
                // given
                String id = null;

                // when & then
                assertThatThrownBy(() -> UserIdentifier.create(id))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("사용자의 ID는 Null이 될 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("value가 영문과 숫자가 아닌 경우")
        class value가_영문_숫자가_아닌_경우 {

            @Test
            @DisplayName("특수문자를 포함하면 예외를 발생시킨다")
            void 특수문자_예외() {
                // given
                String id = "user@123";

                // when & then
                assertThatThrownBy(() -> UserIdentifier.create(id))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자의 ID는 영문 또는 숫자로만 이루어져야합니다.");
            }

            @Test
            @DisplayName("공백을 포함하면 예외를 발생시킨다")
            void 공백_예외() {
                // given
                String id = "user 123";

                // when & then
                assertThatThrownBy(() -> UserIdentifier.create(id))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자의 ID는 영문 또는 숫자로만 이루어져야합니다.");
            }

            @Test
            @DisplayName("한글을 포함하면 예외를 발생시킨다")
            void 한글_예외() {
                // given
                String id = "사용자123";

                // when & then
                assertThatThrownBy(() -> UserIdentifier.create(id))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자의 ID는 영문 또는 숫자로만 이루어져야합니다.");
            }
        }

        @Nested
        @DisplayName("value의 길이가 범위를 벗어나는 경우")
        class value의_길이가_범위를_벗어나는_경우 {

            static Stream<String> invalidLengthIds() {
                return Stream.of(
                        "abcde123456"       // 길이 11 (최대 초과)
                );
            }

            @ParameterizedTest
            @MethodSource("invalidLengthIds")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(String id) {

                assertThatThrownBy(() -> UserIdentifier.create(id))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("1자 이상")
                        .hasMessageContaining("10자 이하");
            }
        }
    }
}
