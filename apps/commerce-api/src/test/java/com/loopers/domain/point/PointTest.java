package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointTest {

    @DisplayName("초기 금액을 지정하여 포인트를 생성할 수 있다.")
    @Test
    void createsPointWithInitialAmount() {
        // act
        Point point = Point.create("testuser01", 1000L);

        // assert
        assertThat(point.getUserId()).isEqualTo("testuser01");
        assertThat(point.getBalanceValue()).isEqualTo(1000L);
    }

    @DisplayName("사용자 ID가 null이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenUserIdIsNull() {
        // act & assert
        assertThatThrownBy(() -> Point.create(null, 0L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("사용자 ID가 빈 문자열이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenUserIdIsEmpty() {
        // act & assert
        assertThatThrownBy(() -> Point.create("", 0L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("포인트 금액이 null이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenAmountIsNull() {
        // act & assert
        assertThatThrownBy(() -> Point.create("testuser01", null))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("포인트 금액이 음수이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenAmountIsNegative() {
        // act & assert
        assertThatThrownBy(() -> Point.create("testuser01", -100L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("포인트를 충전할 수 있다.")
    @Test
    void canChargePoint() {
        // arrange
        Point point = Point.create("testuser01");

        // act
        point.charge(1000L);

        // assert
        assertThat(point.getBalanceValue()).isEqualTo(1000L);
    }

    @DisplayName("여러 번 충전할 수 있다.")
    @Test
    void canChargeMultipleTimes() {
        // arrange
        Point point = Point.create("testuser01");

        // act
        point.charge(1000L);
        point.charge(500L);
        point.charge(300L);

        // assert
        assertThat(point.getBalanceValue()).isEqualTo(1800L);
    }

    @DisplayName("충전 금액이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenChargeAmountIsZeroOrNegative() {
        // arrange
        Point point = Point.create("testuser01");

        // act & assert
        assertThatThrownBy(() -> point.charge(0L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);

        assertThatThrownBy(() -> point.charge(-100L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("충전 금액이 null이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenChargeAmountIsNull() {
        // arrange
        Point point = Point.create("testuser01");

        // act & assert
        assertThatThrownBy(() -> point.charge(null))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("포인트를 사용할 수 있다.")
    @Test
    void canUsePoint() {
        // arrange
        Point point = Point.create("testuser01", 10000L);

        // act
        point.use(3000L);

        // assert
        assertThat(point.getBalanceValue()).isEqualTo(7000L);
    }

    @DisplayName("여러 번 사용할 수 있다.")
    @Test
    void canUseMultipleTimes() {
        // arrange
        Point point = Point.create("testuser01", 10000L);

        // act
        point.use(2000L);
        point.use(3000L);
        point.use(1000L);

        // assert
        assertThat(point.getBalanceValue()).isEqualTo(4000L);
    }

    @DisplayName("잔액보다 많은 금액을 사용하면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenInsufficientBalance() {
        // arrange
        Point point = Point.create("testuser01", 5000L);

        // act & assert
        assertThatThrownBy(() -> point.use(10000L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("사용 금액이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenUseAmountIsZeroOrNegative() {
        // arrange
        Point point = Point.create("testuser01", 10000L);

        // act & assert
        assertThatThrownBy(() -> point.use(0L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);

        assertThatThrownBy(() -> point.use(-100L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("사용 금액이 null이면 BAD_REQUEST 예외가 발생한다.")
    @Test
    void throwsException_whenUseAmountIsNull() {
        // arrange
        Point point = Point.create("testuser01", 10000L);

        // act & assert
        assertThatThrownBy(() -> point.use(null))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }
}
