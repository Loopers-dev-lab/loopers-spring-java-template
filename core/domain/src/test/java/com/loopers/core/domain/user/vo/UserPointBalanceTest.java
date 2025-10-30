package com.loopers.core.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserPointBalance")
class UserPointBalanceTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @ParameterizedTest
            @MethodSource("validPoints")
            @DisplayName("UserPointBalance 객체를 생성한다")
            void 객체_생성(int validPoint) {
                // when
                UserPointBalance pointBalance = new UserPointBalance(validPoint);

                // then
                assertThat(pointBalance.value()).isEqualTo(validPoint);
            }

            static Stream<Integer> validPoints() {
                return Stream.of(0, 1, 100, 10000, Integer.MAX_VALUE);
            }
        }

        @Nested
        @DisplayName("value가 음수인 경우")
        class value가_음수인_경우 {

            @ParameterizedTest
            @MethodSource("invalidPoints")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(int invalidPoint) {
                // when & then
                assertThatThrownBy(() -> new UserPointBalance(invalidPoint))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자 포인트의 잔액은 0보다는 커야합니다.");
            }

            static Stream<Integer> invalidPoints() {
                return Stream.of(-1, -100, -10000, Integer.MIN_VALUE);
            }
        }
    }

    @Nested
    @DisplayName("init() 메서드")
    class InitMethod {

        @Test
        @DisplayName("포인트가 0인 UserPointBalance를 생성한다")
        void 초기_포인트_생성() {
            // when
            UserPointBalance pointBalance = UserPointBalance.init();

            // then
            assertThat(pointBalance.value()).isZero();
        }
    }
}
