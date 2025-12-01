package com.loopers.application.order;

import java.util.List;

/**
 * packageName : com.loopers.application.order
 * fileName     : CreateOrderCommand
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
public record CreateOrderCommand(
        String userId,
        List<OrderItemCommand> items
) {}
