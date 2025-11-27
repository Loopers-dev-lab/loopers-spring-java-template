package com.loopers.application.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.product.ProductSortType;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_DETAIL_KEY = "product::product-id::%s::detail";
    private static final String PRODUCT_LIST_KEY = "product::brand-id=%s::sort=%s::page=%s::size=%s::list";

    public void createOrUpdateDetail(final Long productId, final ProductWithBrandInfo value, final Duration ttl) {
        try {
            String stringValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(generateDetailKey(productId), stringValue, ttl);
        } catch (Exception e) {
            return;
        }

    }

    public void createOrUpdateList(
            final ProductSearchCriteria productSearchCriteria,
            final List<ProductWithBrandInfo> value,
            final Duration ttl) {
        if (productSearchCriteria.page() != 1) {
            return;
        }
        try {
            String stringValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(generateListKey(productSearchCriteria.brandId(), productSearchCriteria.productSortType(), productSearchCriteria.page(), productSearchCriteria.size()), stringValue, ttl);
        } catch (Exception e) {
            return;
        }
    }

    public ProductWithBrandInfo readDetail(final Long productId) {
        try {
            String value = redisTemplate.opsForValue()
                    .get(generateDetailKey(productId));
            return objectMapper.readValue(value, ProductWithBrandInfo.class);

        } catch (Exception e) {
            return null;
        }
    }

    public List<ProductWithBrandInfo> readList(final ProductSearchCriteria productSearchCriteria) {
        if (productSearchCriteria.page() != 1) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue()
                    .get(generateListKey(productSearchCriteria.brandId(), productSearchCriteria.productSortType(), productSearchCriteria.page(), productSearchCriteria.size()));
            return objectMapper.readValue(value, new TypeReference<List<ProductWithBrandInfo>>() {});

        } catch (Exception e) {
            return null;
        }
    }

    public void deleteDetailCache(final Long productId) {
        redisTemplate.delete(generateDetailKey(productId));
    }

    private String generateDetailKey(final Long productId) {
        return PRODUCT_DETAIL_KEY.formatted(productId);
    }

    private String generateListKey(final Long brandId, ProductSortType sortType, Integer page, Integer size) {
        return PRODUCT_LIST_KEY.formatted(brandId, sortType, page, size);
    }
}
