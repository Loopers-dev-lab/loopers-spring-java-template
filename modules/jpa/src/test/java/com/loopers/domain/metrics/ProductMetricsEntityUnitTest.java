package com.loopers.domain.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ProductMetricsEntity 단위 테스트
 *
 * @author hyunjikoh
 * @since 2025. 12. 31.
 */
@DisplayName("ProductMetricsEntity 단위 테스트")
class ProductMetricsEntityUnitTest {

    @Nested
    @DisplayName("엔티티 생성")
    class 엔티티_생성 {

        @Test
        @DisplayName("유효한 정보로 메트릭 엔티티를 생성하면 성공한다")
        void should_create_metrics_entity_successfully_with_valid_information() {
            // given
            Long productId = 1L;
            LocalDate metricDate = LocalDate.of(2024, 12, 31);

            // when
            ProductMetricsEntity entity = ProductMetricsEntity.create(productId, metricDate);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getProductId()).isEqualTo(productId);
            assertThat(entity.getMetricDate()).isEqualTo(metricDate);
            assertThat(entity.getViewCount()).isZero();
            assertThat(entity.getLikeCount()).isZero();
            assertThat(entity.getSalesCount()).isZero();
            assertThat(entity.getOrderCount()).isZero();
        }

        @Test
        @DisplayName("상품 ID가 null이면 예외가 발생한다")
        void should_throw_exception_when_product_id_is_null() {
            // given
            LocalDate metricDate = LocalDate.of(2024, 12, 31);

            // when & then
            assertThatThrownBy(() -> ProductMetricsEntity.create(null, metricDate))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("상품 ID는 필수입니다.");
        }

        @Test
        @DisplayName("메트릭 날짜가 null이면 예외가 발생한다")
        void should_throw_exception_when_metric_date_is_null() {
            // given
            Long productId = 1L;

            // when & then
            assertThatThrownBy(() -> ProductMetricsEntity.create(productId, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("메트릭 날짜는 필수입니다.");
        }
    }

    @Nested
    @DisplayName("조회수 증가")
    class 조회수_증가 {

        @Test
        @DisplayName("조회수가 1 증가한다")
        void should_increment_view_count_by_one() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();

            // when
            entity.incrementView(eventTime);

            // then
            assertThat(entity.getViewCount()).isEqualTo(1L);
            assertThat(entity.getLastEventAt()).isEqualTo(eventTime);
        }

        @Test
        @DisplayName("여러 번 호출하면 조회수가 누적된다")
        void should_accumulate_view_count_on_multiple_calls() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();

            // when
            entity.incrementView(eventTime);
            entity.incrementView(eventTime);
            entity.incrementView(eventTime);

            // then
            assertThat(entity.getViewCount()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("좋아요 수 변경")
    class 좋아요_수_변경 {

        @Test
        @DisplayName("좋아요 수가 증가한다")
        void should_increase_like_count() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();

            // when
            entity.applyLikeDelta(5, eventTime);

            // then
            assertThat(entity.getLikeCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("좋아요 수가 감소한다")
        void should_decrease_like_count() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();
            entity.applyLikeDelta(10, eventTime);

            // when
            entity.applyLikeDelta(-3, eventTime);

            // then
            assertThat(entity.getLikeCount()).isEqualTo(7L);
        }

        @Test
        @DisplayName("좋아요 수는 0 미만으로 내려가지 않는다")
        void should_not_go_below_zero() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();
            entity.applyLikeDelta(5, eventTime);

            // when
            entity.applyLikeDelta(-10, eventTime);

            // then
            assertThat(entity.getLikeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("판매량 증가")
    class 판매량_증가 {

        @Test
        @DisplayName("판매량과 주문 건수가 증가한다")
        void should_increase_sales_and_order_count() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();

            // when
            entity.addSales(5, eventTime);

            // then
            assertThat(entity.getSalesCount()).isEqualTo(5L);
            assertThat(entity.getOrderCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("0 이하의 수량은 무시된다")
        void should_ignore_zero_or_negative_quantity() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();

            // when
            entity.addSales(0, eventTime);
            entity.addSales(-5, eventTime);

            // then
            assertThat(entity.getSalesCount()).isZero();
            assertThat(entity.getOrderCount()).isZero();
        }

        @Test
        @DisplayName("여러 번 호출하면 판매량과 주문 건수가 누적된다")
        void should_accumulate_sales_and_order_count() {
            // given
            ProductMetricsEntity entity = ProductMetricsEntity.create(1L, LocalDate.now());
            ZonedDateTime eventTime = ZonedDateTime.now();

            // when
            entity.addSales(3, eventTime);
            entity.addSales(2, eventTime);

            // then
            assertThat(entity.getSalesCount()).isEqualTo(5L);
            assertThat(entity.getOrderCount()).isEqualTo(2L);
        }
    }
}
