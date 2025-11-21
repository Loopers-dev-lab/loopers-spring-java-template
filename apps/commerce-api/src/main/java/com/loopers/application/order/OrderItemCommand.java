package com.loopers.application.order;

/**
 * packageName : com.loopers.application.order
 * fileName     : OrderItemCommand
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
public record OrderItemCommand(
        Long productId,
        Long quantity
) {}
