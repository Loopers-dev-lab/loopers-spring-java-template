package com.loopers.infrastructure.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMetricsTest {

    @Test
    @DisplayName("ProductMetrics 생성 시 모든 카운트는 0이어야 한다")
    void createTest1() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        ProductMetrics metrics = ProductMetrics.create(1L, date);

        assertThat(metrics.getProductId()).isEqualTo(1L);
        assertThat(metrics.getDate()).isEqualTo(date);
        assertThat(metrics.getViewCount()).isEqualTo(0L);
        assertThat(metrics.getLikeCount()).isEqualTo(0L);
        assertThat(metrics.getOrderCount()).isEqualTo(0L);
        assertThat(metrics.getTotalQuantity()).isEqualTo(0L);
    }

    @Test
    @DisplayName("create(Long) 호출 시 오늘 날짜로 생성된다")
    void createTest2() {
        ProductMetrics metrics = ProductMetrics.create(1L);

        assertThat(metrics.getProductId()).isEqualTo(1L);
        assertThat(metrics.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("incrementViewCount 호출 시 조회수가 1 증가한다")
    void incrementViewCountTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());

        metrics.incrementViewCount();

        assertThat(metrics.getViewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("incrementLikeCount 호출 시 좋아요 수가 1 증가한다")
    void incrementLikeCountTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());

        metrics.incrementLikeCount();

        assertThat(metrics.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("addOrder 호출 시 주문 수와 총 판매 수량이 증가한다")
    void addOrderTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());

        metrics.addOrder(3);

        assertThat(metrics.getOrderCount()).isEqualTo(1L);
        assertThat(metrics.getTotalQuantity()).isEqualTo(3L);
    }

    @Test
    @DisplayName("decrementLikeCount 호출 시 좋아요 수가 1 감소한다")
    void decrementLikeCountTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());
        metrics.incrementLikeCount();
        metrics.incrementLikeCount();

        metrics.decrementLikeCount();

        assertThat(metrics.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("decrementLikeCount 호출 시 0 미만으로 내려가지 않는다")
    void decrementLikeCountTest2() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());

        metrics.decrementLikeCount();

        assertThat(metrics.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("좋아요 이벤트 시간이 더 최신이면 업데이트하고 true 반환")
    void updateLikeIfNewerTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());
        LocalDateTime oldTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 11, 0);
        metrics.updateLikeIfNewer(true, oldTime);

        boolean updated = metrics.updateLikeIfNewer(true, newTime);

        assertThat(updated).isTrue();
        assertThat(metrics.getLikeCount()).isEqualTo(2L);
        assertThat(metrics.getLastLikeEventAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("좋아요 이벤트 시간이 더 오래되면 무시하고 false 반환")
    void updateLikeIfNewerTest2() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 1, 11, 0);
        LocalDateTime oldTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        metrics.updateLikeIfNewer(true, newTime);

        boolean updated = metrics.updateLikeIfNewer(true, oldTime);

        assertThat(updated).isFalse();
        assertThat(metrics.getLikeCount()).isEqualTo(1L);
        assertThat(metrics.getLastLikeEventAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("첫 이벤트는 항상 처리된다")
    void updateLikeIfNewerTest3() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());
        LocalDateTime eventTime = LocalDateTime.of(2024, 1, 1, 10, 0);

        boolean updated = metrics.updateLikeIfNewer(true, eventTime);

        assertThat(updated).isTrue();
        assertThat(metrics.getLikeCount()).isEqualTo(1L);
        assertThat(metrics.getLastLikeEventAt()).isEqualTo(eventTime);
    }

    @Test
    @DisplayName("좋아요 취소도 시간 비교 후 처리된다")
    void updateLikeIfNewerTest4() {
        ProductMetrics metrics = ProductMetrics.create(1L, LocalDate.now());
        LocalDateTime likeTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime unlikeTime = LocalDateTime.of(2024, 1, 1, 11, 0);
        metrics.updateLikeIfNewer(true, likeTime);

        boolean updated = metrics.updateLikeIfNewer(false, unlikeTime);

        assertThat(updated).isTrue();
        assertThat(metrics.getLikeCount()).isEqualTo(0L);
        assertThat(metrics.getLastLikeEventAt()).isEqualTo(unlikeTime);
    }
}
