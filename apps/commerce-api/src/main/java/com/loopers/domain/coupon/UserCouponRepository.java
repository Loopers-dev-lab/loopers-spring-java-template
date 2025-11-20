package com.loopers.domain.coupon;

import java.util.Optional;

/**
 * UserCoupon 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 사용자 쿠폰 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface UserCouponRepository {
    /**
 * Persist the given UserCoupon and return the persisted entity.
 *
 * @param userCoupon the UserCoupon to persist
 * @return the persisted UserCoupon, possibly with updated state (for example, a generated identifier)
 */
    UserCoupon save(UserCoupon userCoupon);

    /**
 * Finds a user coupon by user ID and coupon code.
 *
 * @param userId the user's identifier
 * @param couponCode the coupon's code
 * @return an Optional containing the found UserCoupon, or Optional.empty() if none
 */
    Optional<UserCoupon> findByUserIdAndCouponCode(Long userId, String couponCode);

    /**
 * Retrieves a user's coupon by user ID and coupon code while acquiring a pessimistic write lock.
 *
 * <p>Use when concurrency control is required (for example, when consuming a coupon).</p>
 *
 * <p><b>Lock strategy:</b>
 * <ul>
 *   <li><b>PESSIMISTIC_WRITE:</b> uses SELECT ... FOR UPDATE</li>
 *   <li><b>Lock scope:</b> locks only the row identified by the UNIQUE(userId, couponId) index to minimize locking</li>
 *   <li><b>Purpose:</b> prevents lost updates during coupon usage</li>
 * </ul>
 * </p>
 *
 * @param userId     the identifier of the user
 * @param couponCode the coupon code to look up
 * @return an Optional containing the found UserCoupon if present, or an empty Optional otherwise
 */
    Optional<UserCoupon> findByUserIdAndCouponCodeForUpdate(Long userId, String couponCode);
}
