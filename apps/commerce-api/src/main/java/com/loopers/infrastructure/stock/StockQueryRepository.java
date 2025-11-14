package com.loopers.infrastructure.stock;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.loopers.domain.stock.QStock.stock;

@RequiredArgsConstructor
@Component
public class StockQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 재고 감소 (동시성 안전)
     * @param productId 상품 ID
     * @param decreaseQuantity 감소할 수량
     * @return 업데이트된 행 수 (1이면 성공, 0이면 재고 부족)
     */
    public long decreaseQuantity(Long productId, Long decreaseQuantity) {
        if (decreaseQuantity == null || decreaseQuantity <= 0L) {
            return 0L;
        }

        return queryFactory
                .update(stock)
                .set(stock.quantity, stock.quantity.subtract(decreaseQuantity))
                .where(
                        stock.product.id.eq(productId)
                                .and(stock.quantity.goe(decreaseQuantity))  // 재고가 충분한 경우만
                )
                .execute();
    }
}

