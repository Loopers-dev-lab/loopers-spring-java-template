package com.loopers.application.userbehavior;

import java.time.ZonedDateTime;

public class UserBehaviorEvent {

    public interface BaseEvent {
        Long userId();
        ZonedDateTime timestamp();
    }

    public record ProductViewed(
            Long userId,
            Long productId,
            ZonedDateTime timestamp
    ) implements BaseEvent {
        public static ProductViewed from(Long userId, Long productId) {
            return new ProductViewed(
                    userId,
                    productId,
                    ZonedDateTime.now()
            );
        }
    }

    public record LikeRecorded(
            Long userId,
            Long productId,
            ZonedDateTime timestamp
    ) implements BaseEvent {
        public static LikeRecorded from(Long userId, Long productId) {
            return new LikeRecorded(
                    userId,
                    productId,
                    ZonedDateTime.now()
            );
        }
    }

    public record LikeCancelled(
            Long userId,
            Long productId,
            ZonedDateTime timestamp
    ) implements BaseEvent {
        public static LikeCancelled from(Long userId, Long productId) {
            return new LikeCancelled(
                    userId,
                    productId,
                    ZonedDateTime.now()
            );
        }
    }

    public record OrderCreated(
            Long userId,
            String orderKey,
            Integer originalTotalPrice,
            Integer discountPrice,
            ZonedDateTime timestamp
    ) implements BaseEvent {
        public static OrderCreated from(Long userId, String orderKey, Integer originalTotalPrice, Integer discountPrice) {
            return new OrderCreated(
                    userId,
                    orderKey,
                    originalTotalPrice,
                    discountPrice,
                    ZonedDateTime.now()
            );
        }
    }

    public record PaymentRequested(
            Long userId,
            String orderKey,
            Integer amount,
            ZonedDateTime timestamp
    ) implements BaseEvent {
        public static PaymentRequested from(Long userId, String orderKey, Integer amount) {
            return new PaymentRequested(
                    userId,
                    orderKey,
                    amount,
                    ZonedDateTime.now()
            );
        }
    }
}
