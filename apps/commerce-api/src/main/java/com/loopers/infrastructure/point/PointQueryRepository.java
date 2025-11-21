package com.loopers.infrastructure.point;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.loopers.domain.point.QPoint.point;

@RequiredArgsConstructor
@Component
public class PointQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 포인트 차감 (동시성 안전)
     * @param loginId 사용자 로그인 ID
     * @param deductAmount 차감할 금액
     * @return 업데이트된 행 수 (1이면 성공, 0이면 포인트 부족 또는 사용자 없음)
     */
    public long deduct(String loginId, BigDecimal deductAmount) {
        if (deductAmount == null || deductAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        return queryFactory
                .update(point)
                .set(point.amount, point.amount.subtract(deductAmount))
                .where(
                        point.user.loginId.eq(loginId)          // 사용자 로그인 ID와 일치하는 경우
                        .and(point.amount.goe(deductAmount))    // 포인트가 충분한 경우만
                )
                .execute();
    }

    /**
     * 포인트 충전 (동시성 안전)
     * @param loginId 사용자 로그인 ID
     * @param chargeAmount 충전할 금액
     * @return 업데이트된 행 수 (1이면 성공, 0이면 사용자 없음)
     */
    public long chargeAmount(String loginId, BigDecimal chargeAmount) {
        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        return queryFactory
                .update(point)
                .set(point.amount, point.amount.add(chargeAmount))
                .where(point.user.loginId.eq(loginId))
                .execute();
    }
}

