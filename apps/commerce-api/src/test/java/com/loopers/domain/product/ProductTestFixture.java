package com.loopers.domain.product;

/**
 * 테스트용 고정 데이터 (Fixture) 클래스
 * 모든 Product 관련 테스트에서 사용하는 공통 데이터를 관리
 */
public class ProductTestFixture {

    // 기본 유효한 테스트 데이터
    public static final class ValidProduct {
        public static final String NAME = "테스트 상품";
        public static final Integer PRICE = 10000;
        public static final Integer STOCK = 100;
    }

    // 유효하지 않은 테스트 데이터
    public static final class InvalidProduct {
        public static final String NAME = "";
        public static final Integer PRICE = -1;
        public static final Integer STOCK = -1;
    }

    // 기본 유효한 브랜드 데이터
    public static final class ValidBrand {
        public static final String NAME = "테스트 브랜드";
    }

    // 유효하지 않은 브랜드 데이터
    public static final class InvalidBrand {
        public static final String NAME = "";
    }
}

