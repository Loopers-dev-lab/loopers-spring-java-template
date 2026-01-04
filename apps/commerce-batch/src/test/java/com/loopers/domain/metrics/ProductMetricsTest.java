package com.loopers.domain.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductMetrics 도메인 엔티티 테스트.
 * <p>
 * commerce-batch 모듈의 ProductMetrics 엔티티에 대한 단위 테스트입니다.
 * </p>
 */
class ProductMetricsTest {

    @DisplayName("ProductMetrics는 상품 ID로 생성되며 초기값이 0으로 설정된다")
    @Test
    void createsProductMetricsWithInitialValues() {
        // arrange
        Long productId = 1L;

        // act
        ProductMetrics metrics = new ProductMetrics(productId);

        // assert
        assertThat(metrics.getProductId()).isEqualTo(productId);
        assertThat(metrics.getLikeCount()).isEqualTo(0L);
        assertThat(metrics.getSalesCount()).isEqualTo(0L);
        assertThat(metrics.getViewCount()).isEqualTo(0L);
        assertThat(metrics.getVersion()).isEqualTo(0L);
        assertThat(metrics.getUpdatedAt()).isNotNull();
    }

    @DisplayName("좋아요 수를 증가시킬 수 있다")
    @Test
    void canIncrementLikeCount() throws InterruptedException {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        Long initialLikeCount = metrics.getLikeCount();
        Long initialVersion = metrics.getVersion();
        LocalDateTime initialUpdatedAt = metrics.getUpdatedAt();

        // act
        Thread.sleep(1); // 시간 차이를 보장하기 위한 작은 지연
        metrics.incrementLikeCount();

        // assert
        assertThat(metrics.getLikeCount()).isEqualTo(initialLikeCount + 1);
        assertThat(metrics.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(metrics.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @DisplayName("좋아요 수를 감소시킬 수 있다")
    @Test
    void canDecrementLikeCount() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount(); // 먼저 증가시킴
        Long initialLikeCount = metrics.getLikeCount();
        Long initialVersion = metrics.getVersion();

        // act
        metrics.decrementLikeCount();

        // assert
        assertThat(metrics.getLikeCount()).isEqualTo(initialLikeCount - 1);
        assertThat(metrics.getVersion()).isEqualTo(initialVersion + 1);
    }

    @DisplayName("좋아요 수가 0일 때 감소해도 음수가 되지 않는다 (멱등성 보장)")
    @Test
    void preventsNegativeLikeCount_whenDecrementingFromZero() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        assertThat(metrics.getLikeCount()).isEqualTo(0L);
        Long initialVersion = metrics.getVersion();

        // act
        metrics.decrementLikeCount();

        // assert
        assertThat(metrics.getLikeCount()).isEqualTo(0L);
        assertThat(metrics.getVersion()).isEqualTo(initialVersion); // version도 변경되지 않음
    }

    @DisplayName("판매량을 증가시킬 수 있다")
    @Test
    void canIncrementSalesCount() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        Long initialSalesCount = metrics.getSalesCount();
        Long initialVersion = metrics.getVersion();
        Integer quantity = 5;

        // act
        metrics.incrementSalesCount(quantity);

        // assert
        assertThat(metrics.getSalesCount()).isEqualTo(initialSalesCount + quantity);
        assertThat(metrics.getVersion()).isEqualTo(initialVersion + 1);
    }

    @DisplayName("판매량 증가 시 null이나 0 이하의 수량은 무시된다")
    @Test
    void ignoresInvalidQuantity_whenIncrementingSalesCount() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        Long initialSalesCount = metrics.getSalesCount();
        Long initialVersion = metrics.getVersion();

        // act
        metrics.incrementSalesCount(null);
        metrics.incrementSalesCount(0);
        metrics.incrementSalesCount(-1);

        // assert
        assertThat(metrics.getSalesCount()).isEqualTo(initialSalesCount);
        assertThat(metrics.getVersion()).isEqualTo(initialVersion); // version도 변경되지 않음
    }

    @DisplayName("상세 페이지 조회 수를 증가시킬 수 있다")
    @Test
    void canIncrementViewCount() throws InterruptedException {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        Long initialViewCount = metrics.getViewCount();
        Long initialVersion = metrics.getVersion();
        LocalDateTime initialUpdatedAt = metrics.getUpdatedAt();

        // act
        Thread.sleep(1); // 시간 차이를 보장하기 위한 작은 지연
        metrics.incrementViewCount();

        // assert
        assertThat(metrics.getViewCount()).isEqualTo(initialViewCount + 1);
        assertThat(metrics.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(metrics.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    @DisplayName("여러 메트릭을 연속으로 업데이트할 수 있다")
    @Test
    void canUpdateMultipleMetrics() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);

        // act
        metrics.incrementLikeCount();
        metrics.incrementLikeCount();
        metrics.incrementSalesCount(10);
        metrics.incrementViewCount();
        metrics.decrementLikeCount();

        // assert
        assertThat(metrics.getLikeCount()).isEqualTo(1L);
        assertThat(metrics.getSalesCount()).isEqualTo(10L);
        assertThat(metrics.getViewCount()).isEqualTo(1L);
        assertThat(metrics.getVersion()).isEqualTo(5L); // 5번 업데이트됨
    }

    @DisplayName("이벤트 버전이 메트릭 버전보다 크면 업데이트해야 한다고 판단한다")
    @Test
    void shouldUpdate_whenEventVersionIsGreater() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount(); // version = 1
        Long eventVersion = 2L;

        // act
        boolean result = metrics.shouldUpdate(eventVersion);

        // assert
        assertThat(result).isTrue();
    }

    @DisplayName("이벤트 버전이 메트릭 버전보다 작거나 같으면 업데이트하지 않아야 한다고 판단한다")
    @Test
    void shouldNotUpdate_whenEventVersionIsLessOrEqual() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount(); // version = 1
        metrics.incrementLikeCount(); // version = 2

        // act & assert
        assertThat(metrics.shouldUpdate(1L)).isFalse(); // 이벤트 버전이 더 작음
        assertThat(metrics.shouldUpdate(2L)).isFalse(); // 이벤트 버전이 같음
    }

    @DisplayName("이벤트 버전이 null이면 업데이트해야 한다고 판단한다 (하위 호환성)")
    @Test
    void shouldUpdate_whenEventVersionIsNull() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount(); // version = 1

        // act
        boolean result = metrics.shouldUpdate(null);

        // assert
        assertThat(result).isTrue(); // 하위 호환성을 위해 null이면 업데이트
    }

    @DisplayName("초기 버전(0)인 메트릭은 모든 이벤트 버전에 대해 업데이트해야 한다고 판단한다")
    @Test
    void shouldUpdate_whenMetricsVersionIsZero() {
        // arrange
        ProductMetrics metrics = new ProductMetrics(1L);
        assertThat(metrics.getVersion()).isEqualTo(0L);

        // act & assert
        assertThat(metrics.shouldUpdate(0L)).isFalse(); // 같으면 업데이트 안 함
        assertThat(metrics.shouldUpdate(1L)).isTrue(); // 더 크면 업데이트
        assertThat(metrics.shouldUpdate(100L)).isTrue(); // 더 크면 업데이트
    }
}

