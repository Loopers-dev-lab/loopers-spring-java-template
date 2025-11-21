package com.loopers.support.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    /**
     * 범용 에러
     */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 존재하는 리소스입니다."),

    // 사용자 관련 오류
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 사용자 입니다."),

    // 브랜드 관련 오류
    NOT_FOUND_BRAND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 브랜드입니다."),
    DUPLICATE_BRAND(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 존재하는 브랜드 이름입니다."),

    // 상품 관련 오류
    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 상품입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "재고가 부족합니다."),

    //좋아요 관련 오류
    ALREADY_LIKED_PRODUCT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 좋아요한 상품입니다."),
    NOT_EXIST_LIKED(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "좋아요하지 않은 상품입니다."),

    // 주문 관련 오류
    NOT_FOUND_ORDER(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 주문입니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "유효하지 않은 주문 상태입니다."),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "주문 항목은 최소 1개 이상이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
