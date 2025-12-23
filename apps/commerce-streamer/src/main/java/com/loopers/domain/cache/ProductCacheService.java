package com.loopers.domain.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductCacheService {

    private static final String PRODUCT_DETAIL_PREFIX = "product:detail:";

    private final RedisTemplate<String, Object> redisTemplateMaster;

    public ProductCacheService(
            @Qualifier("redisTemplateObjectMaster") RedisTemplate<String, Object> redisTemplateMaster
    ) {
        this.redisTemplateMaster = redisTemplateMaster;
    }

    public void invalidateProductCache(Long productId) {
        String productDetailKey = getProductDetailKey(productId);
        Boolean deleted = redisTemplateMaster.delete(productDetailKey);
        log.info("상품 상세 캐시 삭제: productId={}, deleted={}", productId, deleted);
    }

    private String getProductDetailKey(Long productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }
}

