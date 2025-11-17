package com.loopers.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 존재하는 리소스입니다."),

    /** Money 도메인 에러 */
    INVALID_MONEY_VALUE(HttpStatus.BAD_REQUEST, "INVALID_MONEY_VALUE", "금액은 비어있을 수 없습니다."),
    NEGATIVE_MONEY_VALUE(HttpStatus.BAD_REQUEST, "NEGATIVE_MONEY_VALUE", "금액은 음수일 수 없습니다."),

    /** User 도메인 에러 */
    INVALID_LOGIN_ID_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_ID_EMPTY", "로그인 ID는 비어있을 수 없습니다."),
    INVALID_LOGIN_ID_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_ID_LENGTH", "로그인 ID는 10자 이내여야 합니다."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_ID_FORMAT", "로그인 ID는 영문/숫자만 허용됩니다."),
    INVALID_EMAIL_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_EMPTY", "이메일은 비어있을 수 없습니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_FORMAT", "이메일 형식이 올바르지 않습니다."),
    INVALID_BIRTH_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_BIRTH_EMPTY", "생년월일은 비어있을 수 없습니다."),
    INVALID_BIRTH_FUTURE(HttpStatus.BAD_REQUEST, "INVALID_BIRTH_FUTURE", "생년월일은 미래일 수 없습니다."),
    INVALID_GENDER_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_GENDER_EMPTY", "성별은 비어있을 수 없습니다."),

    /** Point 도메인 에러 */
    INVALID_POINT_USER_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_POINT_USER_EMPTY", "사용자는 비어있을 수 없습니다."),

    /** PointAmount 도메인 에러 */
    INVALID_POINT_AMOUNT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_POINT_AMOUNT_EMPTY", "포인트 금액은 비어있을 수 없습니다."),
    NEGATIVE_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "NEGATIVE_POINT_AMOUNT", "포인트 금액은 음수일 수 없습니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_CHARGE_AMOUNT", "충전 금액은 0보다 커야 합니다."),
    INVALID_DEDUCT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_DEDUCT_AMOUNT", "차감 금액은 0보다 커야 합니다."),
    INSUFFICIENT_POINT_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINT_BALANCE", "포인트 잔액이 부족합니다."),

    /** Stock 도메인 에러 */
    INVALID_STOCK_VALUE(HttpStatus.BAD_REQUEST, "INVALID_STOCK_VALUE", "재고 수량은 비어있을 수 없습니다."),
    NEGATIVE_STOCK_VALUE(HttpStatus.BAD_REQUEST, "NEGATIVE_STOCK_VALUE", "재고 수량은 음수일 수 없습니다."),
    INVALID_STOCK_INCREASE_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_STOCK_INCREASE_AMOUNT", "증가 수량은 0 이상이어야 합니다."),
    INVALID_STOCK_DECREASE_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_STOCK_DECREASE_AMOUNT", "감소 수량은 0 이상이어야 합니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK", "재고가 부족합니다."),

    /** Brand 도메인 에러 */
    INVALID_BRAND_NAME_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_BRAND_NAME_EMPTY", "브랜드명은 비어있을 수 없습니다."),
    INVALID_BRAND_NAME_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_BRAND_NAME_LENGTH", "브랜드명은 50자 이내여야 합니다."),

    /** Product 도메인 에러 */
    INVALID_PRODUCT_NAME_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_NAME_EMPTY", "상품명은 비어있을 수 없습니다."),
    INVALID_PRODUCT_NAME_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_NAME_LENGTH", "상품명은 100자 이내여야 합니다."),
    INVALID_PRODUCT_PRICE_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_PRICE_EMPTY", "가격은 비어있을 수 없습니다."),
    INVALID_PRODUCT_STOCK_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_STOCK_EMPTY", "재고는 비어있을 수 없습니다."),
    INVALID_PRODUCT_BRAND_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_BRAND_EMPTY", "브랜드는 비어있을 수 없습니다."),

    /** ProductLike 도메인 에러 */
    INVALID_PRODUCT_LIKE_USER_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_LIKE_USER_EMPTY", "사용자는 비어있을 수 없습니다."),
    INVALID_PRODUCT_LIKE_PRODUCT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_LIKE_PRODUCT_EMPTY", "상품은 비어있을 수 없습니다."),
    INVALID_PRODUCT_LIKE_LIKED_AT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_LIKE_LIKED_AT_EMPTY", "좋아요 일시는 비어있을 수 없습니다."),

    /** ProductDetail 도메인 에러 */
    INVALID_PRODUCT_DETAIL_PRODUCT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_DETAIL_PRODUCT_EMPTY", "상품 정보는 필수입니다."),
    INVALID_PRODUCT_DETAIL_BRAND_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_DETAIL_BRAND_EMPTY", "브랜드 정보는 필수입니다."),

    /** Quantity 도메인 에러 */
    INVALID_QUANTITY_VALUE(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY_VALUE", "수량은 비어있을 수 없습니다."),
    INVALID_QUANTITY_RANGE(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY_RANGE", "수량은 0 이상이어야 합니다."),

    /** OrderItem 도메인 에러 */
    INVALID_ORDER_ITEM_ORDER_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ITEM_ORDER_EMPTY", "주문은 비어있을 수 없습니다."),
    INVALID_ORDER_ITEM_PRODUCT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ITEM_PRODUCT_EMPTY", "상품은 비어있을 수 없습니다."),
    INVALID_ORDER_ITEM_PRODUCT_NAME_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ITEM_PRODUCT_NAME_EMPTY", "상품명은 비어있을 수 없습니다."),
    INVALID_ORDER_ITEM_PRODUCT_NAME_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ITEM_PRODUCT_NAME_LENGTH", "상품명은 100자 이내여야 합니다."),
    INVALID_ORDER_ITEM_QUANTITY_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ITEM_QUANTITY_EMPTY", "수량은 비어있을 수 없습니다."),

    /** OrderPrice 도메인 에러 */
    INVALID_ORDER_PRICE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_ORDER_PRICE_VALUE", "주문 가격은 비어있을 수 없습니다."),
    NEGATIVE_ORDER_PRICE_VALUE(HttpStatus.BAD_REQUEST, "NEGATIVE_ORDER_PRICE_VALUE", "주문 가격은 음수일 수 없습니다."),

    /** Order 도메인 에러 */
    INVALID_ORDER_USER_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_USER_EMPTY", "사용자는 비어있을 수 없습니다."),
    INVALID_ORDER_STATUS_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS_EMPTY", "주문 상태는 비어있을 수 없습니다."),
    INVALID_ORDER_TOTAL_AMOUNT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_TOTAL_AMOUNT_EMPTY", "주문 총액은 비어있을 수 없습니다."),
    NEGATIVE_ORDER_TOTAL_AMOUNT(HttpStatus.BAD_REQUEST, "NEGATIVE_ORDER_TOTAL_AMOUNT", "주문 총액은 음수일 수 없습니다."),
    INVALID_ORDER_ORDERED_AT_EMPTY(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ORDERED_AT_EMPTY", "주문 시각은 비어있을 수 없습니다."),
    ORDER_CANNOT_COMPLETE(HttpStatus.BAD_REQUEST, "ORDER_CANNOT_COMPLETE", "PENDING 상태의 주문만 완료할 수 있습니다."),
    ORDER_CANNOT_FAIL_PAYMENT(HttpStatus.BAD_REQUEST, "ORDER_CANNOT_FAIL_PAYMENT", "PENDING 상태의 주문만 결제 실패 상태로 변경할 수 있습니다."),
    ORDER_CANNOT_RETRY_COMPLETE(HttpStatus.BAD_REQUEST, "ORDER_CANNOT_RETRY_COMPLETE", "PAYMENT_FAILED 상태의 주문만 재시도 완료할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
