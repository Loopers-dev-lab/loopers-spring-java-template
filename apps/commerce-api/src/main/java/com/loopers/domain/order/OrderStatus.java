package com.loopers.domain.order;

/**
 * packageName : com.loopers.domain.order
 * fileName     : OrderStatus
 * author      : byeonsungmun
 * date        : 2025. 11. 11.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 11.     byeonsungmun       최초 생성
 */
public enum OrderStatus {

    COMPLETE("결제성공"),
    CANCEL("결제취소"),
    FAIL("결제실패"),
    PENDING("결제중");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return this == COMPLETE;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isCanceled() {
        return this == CANCEL;
    }

    public String description() {
        return description;
    }
}
