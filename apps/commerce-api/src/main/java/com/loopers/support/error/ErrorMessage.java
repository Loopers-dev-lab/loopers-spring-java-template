package com.loopers.support.error;

public final class ErrorMessage {

    private ErrorMessage() {
    }

    public static final String PAYMENT_NOT_FOUND = "결제 정보를 찾을 수 없습니다.";
    public static final String PRODUCT_NOT_FOUND = "해당 상품을 찾을 수 없습니다.";
    public static final String BRAND_NOT_FOUND = "해당 브랜드를 찾을 수 없습니다.";
    public static final String USER_NOT_FOUND = "존재하지 않는 유저 입니다.";
    public static final String ORDER_NOT_FOUND = "해당 주문을 찾을 수 없습니다.";
    public static final String ORDER_ACCESS_DENIED = "해당 주문에 접근할 권한이 없습니다.";
    public static final String COUPON_NOT_FOUND = "쿠폰을 찾을 수 없습니다.";
    public static final String COUPON_NOT_OWNER = "해당 쿠폰의 소유자가 아닙니다.";
    public static final String COUPON_ALREADY_USED = "이미 사용된 쿠폰입니다.";
    public static final String PAYMENT_ACCESS_DENIED = "해당 결제에 접근할 권한이 없습니다.";
}
