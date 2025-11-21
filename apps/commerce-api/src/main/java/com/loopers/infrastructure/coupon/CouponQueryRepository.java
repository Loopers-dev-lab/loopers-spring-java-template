package com.loopers.infrastructure.coupon;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.loopers.domain.coupon.QCoupon.coupon;

@RequiredArgsConstructor
@Component
public class CouponQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 쿠폰 사용 처리 (동시성 안전)
     * @param couponId 쿠폰 ID
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @return 업데이트된 행 수 (1이면 성공, 0이면 쿠폰 사용 불가)
     */
    public long useCoupon(Long couponId, Long orderId, Long userId) {
        if (couponId == null || orderId == null || userId == null) {
            return 0L;
        }

        return queryFactory
                .update(coupon)
                .set(coupon.isUsed, true)
                .set(coupon.order.id, orderId)
                .where(
                        coupon.id.eq(couponId)
                        .and(coupon.isUsed.eq(false))  // 미사용 쿠폰만
                        .and(coupon.order.isNull())  // order가 null이어야 함 (미사용 쿠폰은 order가 null)
                        .and(coupon.user.id.eq(userId))  // 소유자 확인
                        .and(coupon.deletedAt.isNull())  // soft delete 확인
                )
                .execute();
    }
}

