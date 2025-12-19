package com.loopers.infrastructure.event.payloads;

/**
 * 재고 소진 이벤트 페이로드 V1
 * 
 * @author hyunjikoh
 * @since 2025. 12. 19.
 */
public record StockDepletedPayloadV1(
        Long productId,         // 재고 소진된 상품 ID
        Long brandId,           // 브랜드 ID (브랜드별 캐시 무효화용)
        String productName,     // 상품명 (로깅용)
        Integer remainingStock, // 남은 재고 (0이어야 함)
        Long warehouseId        // 창고 ID (선택적)
) {
}