package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class PointTest {

    @Nested
    @DisplayName("포인트 생성 테스트")
    class CreatePointTest {

        @Test
        @DisplayName("포인트 생성 성공")
        void createPointSuccess() {
            // when
            Point point = Point.create("user123", 100L);

            // then
            assertThat(point.getUserId()).isEqualTo("user123");
            assertThat(point.getBalance()).isEqualTo(100L);
        }

        @Test
        @DisplayName("userId가 null이면 생성 실패")
        void createPointFailUserIdNull() {
            assertThatThrownBy(() -> Point.create(null, 100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용자 ID가 비어있을 수 없습니다.");
        }

        @Test
        @DisplayName("userId가 공백이면 생성 실패")
        void createPointFailUserIdBlank() {
            assertThatThrownBy(() -> Point.create("", 100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용자 ID가 비어있을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("포인트 충전 테스트")
    class ChargePointTest {

        @Test
        @DisplayName("포인트 충전 성공")
        void chargeSuccess() {
            // given
            Point point = Point.create("user123", 100L);

            // when
            point.charge(50L);

            // then
            assertThat(point.getBalance()).isEqualTo(150L);
        }

        @Test
        @DisplayName("포인트 충전 실패 - 0 이하 입력")
        void chargeFailZeroOrNegative() {
            Point point = Point.create("user123", 100L);

            assertThatThrownBy(() -> point.charge(0L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("충전");

            assertThatThrownBy(() -> point.charge(-10L))
                    .isInstanceOf(CoreException.class);
        }
    }

    @Nested
    @DisplayName("포인트 사용 테스트")
    class UsePointTest {

        @Test
        @DisplayName("포인트 사용 성공")
        void useSuccess() {
            // given
            Point point = Point.create("user123", 100L);

            // when
            point.use(40L);

            // then
            assertThat(point.getBalance()).isEqualTo(60L);
        }

        @Test
        @DisplayName("포인트 사용 실패 - 0 이하 입력")
        void useFailZeroOrNegative() {
            Point point = Point.create("user123", 100L);

            assertThatThrownBy(() -> point.use(0L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용할 수 없습니다.");

            assertThatThrownBy(() -> point.use(-10L))
                    .isInstanceOf(CoreException.class);
        }

        @Test
        @DisplayName("포인트 사용 실패 - 잔액 부족")
        void useFailNotEnough() {
            Point point = Point.create("user123", 50L);

            assertThatThrownBy(() -> point.use(100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트가 부족");
        }
    }

}
