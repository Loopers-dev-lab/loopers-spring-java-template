package com.loopers.application.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCount;
import com.loopers.domain.product.ProductLikeCountService;
import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.product.ProductV1Dto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductFacade {
    private final ProductService productService;
    private final ProductLikeCountService productLikeCountService;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL_LIST = Duration.ofMinutes(10);
    private static final Duration TTL_DETAIL = Duration.ofMinutes(5);

    public Page<ProductInfo> getProductsV1(Long brandId, Pageable pageable, String sort) {
        return productService.getProductsV1(brandId, pageable, sort);
    }

    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sort) {

        // 좋아요 수 정렬일 경우 MV(productLikeCount) 테이블에서 필터링/정렬 및 조회
        if ("likes_desc".equals(sort)) {
            // 1. MV에서 정렬된 데이터 조회
            Page<ProductLikeCount> productLikeCountsPage = productLikeCountService.getProductLikeCountsOrderByLikeCount(brandId, pageable);

            // 2. 결과 productId 리스트 추출
            List<Long> productIds = productLikeCountsPage.getContent().stream()
                    .map(ProductLikeCount::getProductId)
                    .toList();

            // 3. 추가 product 데이터 조회
            Map<Long, Product> productsById = productService.getProductsMapByIds(productIds);

            // 4. 1번 좋아요 수 정렬 결과에 3번 상품 추가데이터를 조합
            List<ProductInfo> sortedProductInfos = productLikeCountsPage.stream()
                    .map(plc ->
                        ProductInfo.from(
                                productsById.get(plc.getProductId()),
                                plc
                        )
                    )
                    .toList();

            return new PageImpl<>(sortedProductInfos, pageable, productLikeCountsPage.getTotalElements());
        } else {
            // 좋아요 정렬 외엔 product 테이블에서 필터링/정렬 및 조회
            return productService.getProducts(brandId, pageable, sort);
        }
    }

    public Page<ProductInfo> getProductsWithCache(Long brandId, Pageable pageable, String sort) {
        String key = "product:v1:list:brandId=" + brandId
                + ":page=" + pageable.getPageNumber()
                + ":size=" + pageable.getPageSize() + ":sort=" + sort;

        // 1. 캐시 먼저 조회 후 Cache Hit 시 리턴
        try {
            String cashedList = redisTemplate.opsForValue().get(key);
            if (cashedList != null) {
                ProductV1Dto.PageResponse<ProductInfo> pageResponse =
                        objectMapper.readValue(
                                cashedList,
                                new TypeReference<ProductV1Dto.PageResponse<ProductInfo>>() {}
                );

                return new PageImpl<>(pageResponse.content(), pageable, pageResponse.totalElements());
            }
        } catch (JsonProcessingException e) {
            log.warn("캐시 역직렬화 오류, key: {}, error: {}", key, e.getMessage());
        }

        // 2. Cache Miss 시 DB 조회
        //    - 좋아요 수 정렬일 경우 MV(productLikeCount) 테이블에서 필터링/정렬 및 조회
        Page<ProductInfo> resultPage;
        if ("likes_desc".equals(sort)) {
            // 1. MV에서 정렬된 데이터 조회
            Page<ProductLikeCount> productLikeCountsPage = productLikeCountService.getProductLikeCountsOrderByLikeCount(brandId, pageable);

            // 2. 결과 productId 리스트 추출
            List<Long> productIds = productLikeCountsPage.getContent().stream()
                    .map(ProductLikeCount::getProductId)
                    .toList();

            // 3. 추가 product 데이터 조회
            Map<Long, Product> productsById = productService.getProductsMapByIds(productIds);

            // 4. 1번 좋아요 수 정렬 결과에 3번 상품 추가데이터를 조합
            List<ProductInfo> sortedProductInfos = productLikeCountsPage.stream()
                    .map(plc ->
                            ProductInfo.from(
                                    productsById.get(plc.getProductId()),
                                    plc
                            )
                    )
                    .toList();

            resultPage = new PageImpl<>(sortedProductInfos, pageable, productLikeCountsPage.getTotalElements());
        } else {
            // 좋아요 정렬 외엔 product 테이블에서 필터링/정렬 및 조회
            resultPage = productService.getProducts(brandId, pageable, sort);
        }

        // 3. DB 조회 결과 캐시 저장
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ProductV1Dto.PageResponse.from(resultPage)), TTL_LIST);
        } catch (JsonProcessingException e) {
            log.warn("Json 직렬화 오류, error: {}", e.getMessage());
        }

        return resultPage;
    }


    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId) {
        return productService.getProductDetail(productId);
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetailWithCache(Long productId) {
        String key = "product:v1:detail:" + productId;

        // 1. 캐시 먼저 조회 후 Cache Hit 시 리턴
        try {
            String cachedDetail = redisTemplate.opsForValue().get(key);
            if (cachedDetail != null) {
                return objectMapper.readValue(cachedDetail, ProductDetailInfo.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("캐시 역직렬화 오류, key: {}, error: {}", key, e.getMessage());
        }

        // 2. Cache Miss 시 DB 조회
        ProductDetailInfo detailInfo = productService.getProductDetail(productId);

        // 3. DB 조회 결과 캐시 저장
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(detailInfo), TTL_DETAIL);
        } catch (JsonProcessingException e) {
            log.warn("Json 직렬화 오류, error: {}", e.getMessage());
        }

        return detailInfo;
    }
}
