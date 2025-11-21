package com.loopers.support;

/**
 * API 엔드포인트 URI 상수 관리 클래스
 *
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public class Uris {

    private Uris() {
        // Utility class
    }

    /**
     * API 버전 상수
     */
    public static final String API_V1 = "/api/v1";

    /**
     * Example API 엔드포인트
     */
    public static class Example {
        private Example() {
        }

        public static final String BASE = API_V1 + "/examples";
        public static final String GET_BY_ID = BASE + "/{exampleId}";
    }

    /**
     * User API 엔드포인트
     */
    public static class User {
        private User() {
        }

        public static final String BASE = API_V1 + "/users";
        public static final String REGISTER = BASE;
        public static final String GET_BY_USERNAME = BASE;
    }

    /**
     * Point API 엔드포인트
     */
    public static class Point {
        private Point() {
        }

        public static final String BASE = API_V1 + "/points";
        public static final String GET_INFO = BASE;
        public static final String CHARGE = BASE + "/charge";
    }

    /**
     * Order API 엔드포인트
     */
    public static class Order {
        /**
         * Prevents instantiation of the Order utility class.
         * <p>
         * This class contains only constant endpoint values.
         */
        private Order() {
        }

        public static final String BASE = API_V1 + "/orders";
        public static final String CREATE = BASE;
        public static final String GET_LIST = BASE;
        public static final String GET_DETAIL = BASE + "/{orderId}";
    }

    /**
     * Product API 엔드포인트
     */
    public static class Product {
        /**
         * Prevents instantiation of the Product URI constants holder.
         */
        private Product() {
        }

        public static final String BASE = API_V1 + "/products";
        public static final String GET_LIST = BASE;
        public static final String GET_DETAIL = BASE + "/{productId}";
    }

    /**
     * Like API 엔드포인트
     */
    public static class Like {
        /**
         * Prevents instantiation of the Like utility class.
         */
        private Like() {
        }

        public static final String BASE = API_V1 + "/likes";
        public static final String UPSERT = BASE + "/products/{productId}";
        public static final String CANCEL = BASE + "/products/{productId}";
    }
}