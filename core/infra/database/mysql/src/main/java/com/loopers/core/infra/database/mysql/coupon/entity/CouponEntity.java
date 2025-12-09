package com.loopers.core.infra.database.mysql.coupon.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.AbstractCoupon;
import com.loopers.core.domain.order.DefaultCoupon;
import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.user.vo.UserId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Getter
@Entity
@Table(
        name = "coupon",
        indexes = {
                @Index(name = "idx_coupon_user_id", columnList = "user_id"),
                @Index(name = "idx_coupon_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Long version;

    public static CouponEntity from(AbstractCoupon coupon) {
        return new CouponEntity(
                Optional.ofNullable(coupon.getCouponId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(coupon.getUserId().value())),
                coupon.getStatus().name(),
                coupon.getCreatedAt().value(),
                coupon.getUpdatedAt().value(),
                coupon.getDeletedAt().value(),
                coupon.getVersion()
        );
    }

    public AbstractCoupon to() {
        return DefaultCoupon.mappedBy(
                new CouponId(this.id.toString()),
                new UserId(this.userId.toString()),
                CouponStatus.valueOf(this.status),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt),
                this.version
        );
    }
}
