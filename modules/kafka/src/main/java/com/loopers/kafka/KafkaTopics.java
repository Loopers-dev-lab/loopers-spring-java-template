package com.loopers.kafka;

public final class KafkaTopics {

    public static final String PRODUCT_LIKE = "product.like";
    public static final String PRODUCT = "product";
    public static final String ORDER = "order";
    public static final String COUPON = "coupon";
    public static final String USER_ACTIVITY = "user.activity";

    // Dead Letter Queue (DLQ) 토픽
    public static final String PRODUCT_LIKE_DLQ = "product.like.DLQ";
    public static final String PRODUCT_DLQ = "product.DLQ";
    public static final String ORDER_DLQ = "order.DLQ";

    public static final class ProductLike {
        public static final String LIKE_ADDED = "LikeAdded";
        public static final String LIKE_REMOVED = "LikeRemoved";

        private ProductLike() {
            throw new AssertionError("Cannot instantiate utility class");
        }
    }

    public static final class ProductDetail {
        public static final String PRODUCT_VIEWED = "ProductViewed";

        private ProductDetail() {
            throw new AssertionError("Cannot instantiate utility class");
        }
    }

    public static final class Order {
        public static final String ORDER_CREATED = "OrderCreated";
        public static final String ORDER_COMPLETED = "OrderCompleted";
        public static final String ORDER_CANCELLED = "OrderCancelled";

        private Order() {
            throw new AssertionError("Cannot instantiate utility class");
        }
    }

    public static final class Coupon {
        public static final String COUPON_USED = "CouponUsed";
        public static final String COUPON_EXPIRED = "CouponExpired";

        private Coupon() {
            throw new AssertionError("Cannot instantiate utility class");
        }
    }

    public static final class UserActivity {
        public static final String USER_ACTIVITY = "UserActivity";

        private UserActivity() {
            throw new AssertionError("Cannot instantiate utility class");
        }
    }

    private KafkaTopics() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
