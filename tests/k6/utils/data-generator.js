/**
 * k6 테스트 데이터 생성 유틸리티
 */

/**
 * 주문 요청 생성
 */
export function generateOrderRequest(productIdRange = { min: 1, max: 1000 }, itemCount = 1) {
  const items = [];
  const usedProductIds = new Set();
  
  for (let i = 0; i < itemCount; i++) {
    let productId;
    // 중복되지 않는 productId 생성
    do {
      productId = Math.floor(
        Math.random() * (productIdRange.max - productIdRange.min + 1) + productIdRange.min
      );
    } while (usedProductIds.has(productId));
    
    usedProductIds.add(productId);
    
    items.push({
      productId: productId,
      quantity: Math.floor(Math.random() * 5) + 1, // 1~5개
    });
  }
  
  return {
    items: items,
    // couponIds: Math.random() > 0.7 ? [] : null, // 30% 확률로 쿠폰 사용 안 함
    couponIds: [],
  };
}

/**
 * 랜덤 Product ID 반환
 */
export function getRandomProductId(min = 1, max = 1000) {
  return Math.floor(Math.random() * (max - min + 1) + min);
}

/**
 * 랜덤 User ID 반환
 */
export function getRandomUserId(min = 1, max = 100) {
  return Math.floor(Math.random() * (max - min + 1) + min);
}

/**
 * 멱등성 키 생성 (실제 API와 동일한 로직)
 */
export function generateIdempotentKey(userId, items, couponIds = null) {
  const itemsString = items
    .map(item => `${item.productId}:${item.quantity}`)
    .sort()
    .join(',');
  
  const couponString = couponIds && couponIds.length > 0
    ? couponIds.sort().join(',')
    : '';
  
  return `${userId}:${itemsString}:${couponString}`;
}

