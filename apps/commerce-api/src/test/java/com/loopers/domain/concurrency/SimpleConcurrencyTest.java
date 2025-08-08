package com.loopers.domain.concurrency;

import com.loopers.domain.points.PointsModel;
import com.loopers.domain.product.ProductModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DisplayName("동시성 제어 단위 테스트")
class SimpleConcurrencyTest {

    @Test
    @DisplayName("상품 재고 차감 시 음수 재고나 부족한 재고에 대해 예외가 발생한다")
    void 상품_재고_차감_예외_테스트() {
        ProductModel product = ProductModel.register(
            "테스트 상품",
            1L,
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(1000),
            "테스트 상품입니다",
            "test.jpg",
            "ACTIVE",
            BigDecimal.ZERO
        );

        assertThatThrownBy(() -> product.decreaseStock(BigDecimal.valueOf(15)))
            .hasMessageContaining("재고가 부족합니다");

        assertThatThrownBy(() -> product.decreaseStock(BigDecimal.valueOf(-1)))
            .hasMessageContaining("차감할 재고량은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("상품 재고 차감 후 상태가 올바르게 변경된다")
    void 상품_재고_차감_상태_변경_테스트() {
        ProductModel product = ProductModel.register(
            "테스트 상품",
            1L,
            BigDecimal.valueOf(1),
            BigDecimal.valueOf(1000),
            "테스트 상품입니다",
            "test.jpg",
            "ACTIVE",
            BigDecimal.ZERO
        );

        product.decreaseStock(BigDecimal.valueOf(1));

        assertThat(product.getStatus().getValue()).isEqualTo("OUT_OF_STOCK");
        assertThat(product.getStock().getValue()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("상품 재고 복구 시 품절 상태에서 활성 상태로 변경된다")
    void 상품_재고_복구_상태_변경_테스트() {
        ProductModel product = ProductModel.register(
            "테스트 상품",
            1L,
            BigDecimal.ZERO,
            BigDecimal.valueOf(1000),
            "테스트 상품입니다",
            "test.jpg",
            "OUT_OF_STOCK",
            BigDecimal.ZERO
        );

        product.restoreStock(BigDecimal.valueOf(5));

        assertThat(product.getStatus().getValue()).isEqualTo("ACTIVE");
        assertThat(product.getStock().getValue()).isEqualTo(BigDecimal.valueOf(5));
    }

    @Test
    @DisplayName("포인트 차감 시 잔액 부족하면 예외가 발생한다")
    void 포인트_차감_예외_테스트() {
        PointsModel points = PointsModel.from(1L, BigDecimal.valueOf(10000));

        assertThatThrownBy(() -> points.deductPoint(BigDecimal.valueOf(20000)))
            .hasMessageContaining("포인트가 부족합니다");

        assertThatThrownBy(() -> points.deductPoint(BigDecimal.valueOf(-1000)))
            .hasMessageContaining("차감할 포인트는 0보다 커야 합니다");
    }

    @Test
    @DisplayName("포인트 충전과 차감이 정상적으로 동작한다")
    void 포인트_충전_차감_테스트() {
        PointsModel points = PointsModel.from(1L, BigDecimal.ZERO);

        points.chargePoint(BigDecimal.valueOf(10000));

        assertThat(points.getPoint()).isEqualTo(BigDecimal.valueOf(10000));

        points.deductPoint(BigDecimal.valueOf(3000));

        assertThat(points.getPoint()).isEqualTo(BigDecimal.valueOf(7000));

        assertThat(points.hasEnoughPoint(BigDecimal.valueOf(5000))).isTrue();
        assertThat(points.hasEnoughPoint(BigDecimal.valueOf(8000))).isFalse();
    }

    @Test
    @DisplayName("상품 좋아요 카운트가 정상적으로 증감한다")
    void 상품_좋아요_카운트_테스트() {
        ProductModel product = ProductModel.register(
            "테스트 상품",
            1L,
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(1000),
            "테스트 상품입니다",
            "test.jpg",
            "ACTIVE",
            BigDecimal.ZERO
        );

        product.incrementLikeCount();
        product.incrementLikeCount();
        product.incrementLikeCount();

        assertThat(product.getLikeCount().getValue()).isEqualTo(BigDecimal.valueOf(3));

        product.decrementLikeCount();

        assertThat(product.getLikeCount().getValue()).isEqualTo(BigDecimal.valueOf(2));
    }
}
