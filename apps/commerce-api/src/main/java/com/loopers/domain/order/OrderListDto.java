package com.loopers.domain.order;

import java.time.LocalDateTime;

/**
 * Order 목록 조회용 DTO (페이징)
 * DTO 프로젝션 + SIZE() 사용으로 N+1 및 메모리 페이징 방지
 */
public record OrderListDto(
    Long id,
    Long userId,
    OrderStatus status,
    Long totalAmount,
    LocalDateTime orderedAt,
    Integer itemCount
) {
}
