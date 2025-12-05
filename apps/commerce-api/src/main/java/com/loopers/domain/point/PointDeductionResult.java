package com.loopers.domain.point;

/**
 * 포인트 차감 결과를 담는 record.
 * 실제 차감된 포인트와 남은 금액을 반환
 *
 * @param deductedAmount 실제 차감된 포인트 금액
 * @param remainingToPay PG 결제가 필요한 잔여 금액 (= pgAmount)
 */
public record PointDeductionResult(
    Long deductedAmount,
    Long remainingToPay
) {
}
