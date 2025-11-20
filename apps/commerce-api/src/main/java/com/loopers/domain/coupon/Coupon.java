package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.discount.CouponDiscountStrategy;
import com.loopers.domain.coupon.discount.CouponDiscountStrategyFactory;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 도메인 엔티티.
 * <p>
 * 쿠폰의 기본 정보(코드, 타입, 할인 금액/비율)를 관리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon extends BaseEntity {
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    /**
     * Coupon 인스턴스를 생성합니다.
     *
     * @param code 쿠폰 코드 (필수, 최대 50자)
     * @param type 쿠폰 타입 (필수)
     * @param discountValue 할인 값 (필수, 0 초과)
     *                      - FIXED_AMOUNT: 할인 금액
     *                      - PERCENTAGE: 할인 비율 (0-100)
     * @throws CoreException 유효성 검증 실패 시
     */
    public Coupon(String code, CouponType type, Integer discountValue) {
        validateCode(code);
        validateType(type);
        validateDiscountValue(type, discountValue);
        this.code = code;
        this.type = type;
        this.discountValue = discountValue;
    }

    /**
     * Coupon 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param code 쿠폰 코드
     * @param type 쿠폰 타입
     * @param discountValue 할인 값
     * @return 생성된 Coupon 인스턴스
     * @throws CoreException 유효성 검증 실패 시
     */
    public static Coupon of(String code, CouponType type, Integer discountValue) {
        return new Coupon(code, type, discountValue);
    }

    /**
     * 쿠폰 코드의 유효성을 검증합니다.
     *
     * @param code 검증할 쿠폰 코드
     * @throws CoreException code가 null, 공백이거나 50자를 초과할 경우
     */
    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 코드는 필수입니다.");
        }
        if (code.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 코드는 50자를 초과할 수 없습니다.");
        }
    }

    /**
     * 쿠폰 타입의 유효성을 검증합니다.
     *
     * @param type 검증할 쿠폰 타입
     * @throws CoreException type이 null일 경우
     */
    private void validateType(CouponType type) {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입은 필수입니다.");
        }
    }

    /**
     * 할인 값의 유효성을 검증합니다.
     *
     * @param type 쿠폰 타입
     * @param discountValue 검증할 할인 값
     * @throws CoreException discountValue가 null이거나 유효하지 않을 경우
     */
    private void validateDiscountValue(CouponType type, Integer discountValue) {
        if (discountValue == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 필수입니다.");
        }
        if (discountValue <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }
        if (type == CouponType.PERCENTAGE && discountValue > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 쿠폰의 할인 비율은 100을 초과할 수 없습니다.");
        }
    }

    /**
     * 주문 금액에 쿠폰을 적용하여 할인 금액을 계산합니다.
     * <p>
     * 전략 패턴을 사용하여 쿠폰 타입별 할인 계산 로직을 분리합니다.
     * 새로운 쿠폰 타입이 추가되어도 기존 코드를 수정하지 않고 확장할 수 있습니다.
     * </p>
     *
     * @param orderAmount 주문 금액
     * @param strategyFactory 할인 계산 전략 팩토리
     * @return 할인 금액
     * @throws CoreException orderAmount가 null이거나 0 이하일 경우
     */
    public Integer calculateDiscountAmount(Integer orderAmount, CouponDiscountStrategyFactory strategyFactory) {
        if (orderAmount == null || orderAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 금액은 0보다 커야 합니다.");
        }

        // 전략 패턴을 사용하여 쿠폰 타입별 할인 계산
        CouponDiscountStrategy strategy = strategyFactory.getStrategy(this.type);
        return strategy.calculateDiscountAmount(orderAmount, this.discountValue);
    }
}

