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

    PENDING("주문중"),
    FAIL("주문실패"),
    COMPLETE("주문성공");

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

    public String description() {
        return description;
    }
}
