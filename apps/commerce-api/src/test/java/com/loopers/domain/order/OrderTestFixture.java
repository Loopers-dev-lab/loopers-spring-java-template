package com.loopers.domain.order;

import java.util.List;

/**
 * 테스트용 고정 데이터 (Fixture) 클래스
 * 모든 Order 관련 테스트에서 사용하는 공통 데이터를 관리
 */
public class OrderTestFixture {

    // 기본 유효한 테스트 데이터
    public static final class ValidOrder {
        public static final Long USER_ID = 1L;
        public static final Integer TOTAL_AMOUNT = 20000;
    }

    // 기본 유효한 주문 아이템 데이터
    public static final class ValidOrderItem {
        public static final Long PRODUCT_ID_1 = 1L;
        public static final String NAME_1 = "테스트 상품 1";
        public static final Integer PRICE_1 = 10000;
        public static final Integer QUANTITY_1 = 1;

        public static final Long PRODUCT_ID_2 = 2L;
        public static final String NAME_2 = "테스트 상품 2";
        public static final Integer PRICE_2 = 5000;
        public static final Integer QUANTITY_2 = 2;

        public static List<OrderItem> createMultipleItems() {
            return List.of(
                OrderItem.of(PRODUCT_ID_1, NAME_1, PRICE_1, QUANTITY_1),
                OrderItem.of(PRODUCT_ID_2, NAME_2, PRICE_2, QUANTITY_2)
            );
        }
    }

}


