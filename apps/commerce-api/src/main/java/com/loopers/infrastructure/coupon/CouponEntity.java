package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 JPA Entity
 */
@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 100)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType couponType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private Boolean isUsed;

    @Column
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Version
    private Long version; // 낙관적 락을 위한 버전 필드

    // === 생성자 ===

    private CouponEntity(
            String userId,
            String couponName,
            CouponType couponType,
            BigDecimal discountValue,
            Boolean isUsed,
            LocalDateTime usedAt,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        this.userId = userId;
        this.couponName = couponName;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static CouponEntity from(Coupon coupon) {
        return new CouponEntity(
                coupon.getUserId(),
                coupon.getCouponName(),
                coupon.getCouponType(),
                coupon.getDiscountValue(),
                coupon.isUsed(),
                coupon.getUsedAt(),
                coupon.getIssuedAt(),
                coupon.getExpiresAt()
        );
    }

    /**
     * Entity를 Domain 객체로 변환
     */
    public Coupon toDomain() {
        return Coupon.reconstruct(
                this.id,
                this.userId,
                this.couponName,
                this.couponType,
                this.discountValue,
                this.isUsed,
                this.usedAt,
                this.issuedAt,
                this.expiresAt
        );
    }

    /**
     * Domain 객체로부터 Entity 업데이트
     */
    public void updateFrom(Coupon coupon) {
        this.isUsed = coupon.isUsed();
        this.usedAt = coupon.getUsedAt();
    }
}
