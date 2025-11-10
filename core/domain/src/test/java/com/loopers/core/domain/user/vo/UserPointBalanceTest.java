package com.loopers.core.domain.user.vo;

import com.loopers.core.domain.payment.vo.PayAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사용자 포인트 잔액")
class UserPointBalanceTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            static Stream<Integer> validPoints() {
                return Stream.of(0, 1, 100, 10000, Integer.MAX_VALUE);
            }

            @ParameterizedTest
            @MethodSource("validPoints")
            @DisplayName("UserPointBalance 객체를 생성한다")
            void 객체_생성(int validPoint) {
                // when
                UserPointBalance pointBalance = new UserPointBalance(new BigDecimal(validPoint));

                // then
                assertThat(pointBalance.value().intValue()).isEqualTo(validPoint);
            }
        }

        @Nested
        @DisplayName("value가 음수인 경우")
        class value가_음수인_경우 {

            static Stream<Integer> invalidPoints() {
                return Stream.of(-1, -100, -10000, Integer.MIN_VALUE);
            }

            @ParameterizedTest
            @MethodSource("invalidPoints")
            @DisplayName("예외를 발생시킨다")
            void 예외_발생(int invalidPoint) {
                // when & then
                assertThatThrownBy(() -> new UserPointBalance(new BigDecimal(invalidPoint)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자 포인트의 잔액는(은) 음수가 될 수 없습니다.");
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

    @Nested
    @DisplayName("add() 메서드")
    class AddMethod {

        @Nested
        @DisplayName("양수 포인트를 추가하는 경우")
        class positiveAdd {

            @Test
            @DisplayName("포인트가 추가된다.")
            void addPoint() {
                UserPointBalance userPointBalance = new UserPointBalance(new BigDecimal(100));
                UserPointBalance addBalance = userPointBalance.add(new BigDecimal(10));
                assertThat(addBalance.value().intValue()).isEqualTo(110);
            }
        }

        @Nested
        @DisplayName("음수 포인트를 추가하는 경우")
        class negativeAdd {

            @Test
            @DisplayName("예외가 발생한다.")
            void throwException() {
                UserPointBalance userPointBalance = new UserPointBalance(new BigDecimal(100));
                assertThatThrownBy(() -> userPointBalance.add(new BigDecimal(-1)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("음수 포인트를 충전할 수 없습니다.");
            }
        }

    }

    @Nested
    @DisplayName("decrease(PayAmount payAmount) 메서드")
    class DecreaseMethod {

        @Nested
        @DisplayName("충분한 포인트가 있는 경우")
        class 충분한_포인트 {

            static Stream<BigDecimal> sufficientCases() {
                return Stream.of(
                    new BigDecimal(100),
                    new BigDecimal(50),
                    new BigDecimal(1)
                );
            }

            @ParameterizedTest
            @MethodSource("sufficientCases")
            @DisplayName("포인트가 차감된다")
            void decreasePoint(BigDecimal decreaseAmount) {
                // given
                UserPointBalance balance = new UserPointBalance(new BigDecimal(100));
                PayAmount payAmount = new PayAmount(decreaseAmount);

                // when
                UserPointBalance decreased = balance.decrease(payAmount);

                // then
                assertThat(decreased.value()).isEqualByComparingTo(
                    new BigDecimal(100).subtract(decreaseAmount)
                );
            }
        }

        @Nested
        @DisplayName("포인트가 부족한 경우")
        class 부족한_포인트 {

            static Stream<BigDecimal> insufficientCases() {
                return Stream.of(
                    new BigDecimal(101),
                    new BigDecimal(1000),
                    new BigDecimal("100.01")
                );
            }

            @ParameterizedTest
            @MethodSource("insufficientCases")
            @DisplayName("예외가 발생한다")
            void throwException(BigDecimal decreaseAmount) {
                // given
                UserPointBalance balance = new UserPointBalance(new BigDecimal(100));
                PayAmount payAmount = new PayAmount(decreaseAmount);

                // when & then
                assertThatThrownBy(() -> balance.decrease(payAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자의 포인트 잔액이 충분하지 않습니다.");
            }
        }

        @Nested
        @DisplayName("차감하려는 포인트가 정확히 현재 포인트와 같은 경우")
        class 정확히_같은_포인트 {

            @Test
            @DisplayName("포인트가 0이 되어 차감된다")
            void decreaseToZero() {
                // given
                UserPointBalance balance = new UserPointBalance(new BigDecimal(100));
                PayAmount payAmount = new PayAmount(new BigDecimal(100));

                // when
                UserPointBalance decreased = balance.decrease(payAmount);

                // then
                assertThat(decreased.value()).isZero();
            }
        }
    }
}
