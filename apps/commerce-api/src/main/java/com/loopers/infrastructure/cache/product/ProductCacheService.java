package com.loopers.infrastructure.cache.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductInfo;
import com.loopers.infrastructure.cache.GlobalCache;
import com.loopers.infrastructure.cache.RedisCacheKeyConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductCacheService {
    private final ObjectMapper objectMapper;
    private final GlobalCache globalCache;

    // keys
    private String getProductDetailKey(Long productId) {
        return RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId);
    }

    private String getProductListKey(Long brandId, int page, int size, String sort) {
        if (brandId == null) {
            return RedisCacheKeyConfig.PRODUCT_LIST.generateKey("all", page, size, sort);
        }
        return RedisCacheKeyConfig.PRODUCT_LIST.generateKey("brand", brandId, page, size, sort);
    }

    private String getProductListKeyPattern() {
        return RedisCacheKeyConfig.PRODUCT_LIST.generateKey("*");
    }

    private String getProductListKeyPatternByBrandId(Long brandId) {
        if (brandId == null) {
            return RedisCacheKeyConfig.PRODUCT_LIST.generateKey("all", "*");
        }
        return RedisCacheKeyConfig.PRODUCT_LIST.generateKey("brand", brandId, "*");
    }

    // deserialize

    private Optional<Optional<ProductInfo>> deserializeProductInfo(String productInfoString) {
        if (StringUtils.isBlank(productInfoString)) {
            return Optional.empty();
        }
        try {
            Optional<ProductInfo> productInfo = objectMapper.readValue(
                    productInfoString,
            objectMapper.getTypeFactory().constructParametricType(Optional.class, ProductInfo.class)
            );
            return Optional.ofNullable(productInfo);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Page<ProductInfo>> deserializeProductInfoList(String productInfoListString) {
        if (StringUtils.isBlank(productInfoListString)) {
            return Optional.empty();
        }
        try {
            Page<ProductInfo> productInfoList = objectMapper.readValue(
                    productInfoListString,
                    objectMapper.getTypeFactory().constructParametricType(Page.class, ProductInfo.class)
            );
            return Optional.ofNullable(productInfoList);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
            return Optional.empty();
        }
    }

    // cache operations
    public Optional<Optional<ProductInfo>> getProductDetail(Long productId) {
        String key = getProductDetailKey(productId);
        return deserializeProductInfo(globalCache.get(key));
    }

    public void setProductDetail(Long productId, Optional<ProductInfo> productInfo) {
        try {
            String key = getProductDetailKey(productId);
            String serialized = objectMapper.writeValueAsString(productInfo);
            globalCache.set(key, serialized, RedisCacheKeyConfig.PRODUCT_DETAIL.expireSeconds);
        } catch (JsonProcessingException e) {
            System.out.println("redis 직렬화 오류 in setProductDetail: " + e.getMessage());
        }
    }

    public void invalidateProductDetail(Long productId) {
        String key = getProductDetailKey(productId);
        globalCache.delete(key);
    }

    public Optional<Page<ProductInfo>> getProductList(Long brandId, int page, int size, String sort) {
        String key = getProductListKey(brandId, page, size, sort);
        return deserializeProductInfoList(globalCache.get(key));
    }

    public void setProductList(Long brandId, int page, int size, String sort, Page<ProductInfo> productInfoList) {
        try {
            String key = getProductListKey(brandId, page, size, sort);
            String serialized = objectMapper.writeValueAsString(productInfoList);
            globalCache.set(key, serialized, RedisCacheKeyConfig.PRODUCT_LIST.expireSeconds);
        } catch (JsonProcessingException e) {
            System.out.println("redis 직렬화 오류 in setProductList: " + e.getMessage());
        }
    }

    public void invalidateProductList(Long brandId) {
        String pattern = getProductListKeyPatternByBrandId(brandId);
        globalCache.deleteByPattern(pattern);
    }

}
