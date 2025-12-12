package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.product.ProductDetailService;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import com.loopers.infrastructure.cache.ProductCacheService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품 애플리케이션 파사드
 * - 트랜잭션 경계, DTO 변환, 인증정보 전달 등 애플리케이션 관심사를 담당한다.
 * - 도메인 규칙과 협력은 ProductDetailService에 위임한다.
 */
public class ProductFacade {

    private final ProductDetailService productDetailService;
    private final ProductCacheService productCacheService;
    private final ProductRepository productRepository;

    public ProductFacade(ProductDetailService productDetailService,
                         ProductCacheService productCacheService,
                         ProductRepository productRepository) {
        this.productDetailService = productDetailService;
        this.productCacheService = productCacheService;
        this.productRepository = productRepository;
    }

    /**
     * 상품 상세 조회
     * - Redis 캐시를 먼저 확인하고, 없으면 DB 조회 후 캐시에 저장한다.
     * @param productId 상품 ID
     * @param userIdOrNull 인증된 사용자 ID (없으면 null)
     */
    public ProductDetailInfo getProductDetail(Long productId, String userIdOrNull) {
        // 캐시 조회
        Optional<ProductDetailInfo> cached = productCacheService.getProductDetail(productId, userIdOrNull);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 캐시 미스: DB 조회
        ProductDetail detail = productDetailService.getProductDetail(productId, userIdOrNull);
        ProductDetailInfo info = ProductDetailInfo.from(detail);

        // 캐시에 저장
        productCacheService.cacheProductDetail(productId, userIdOrNull, info);

        return info;
    }

    /**
     * 상품 목록 조회
     * - Redis 캐시를 먼저 확인하고, 없으면 DB 조회 후 캐시에 저장한다.
     * @param sortType 정렬 타입
     * @param brandIdOrNull 브랜드 ID (없으면 null, 전체 조회)
     * @param userIdOrNull 인증된 사용자 ID (없으면 null)
     */
    public List<ProductDetailInfo> getProductList(ProductSortType sortType, Long brandIdOrNull, String userIdOrNull) {
        // 캐시 조회
        Optional<List<ProductDetailInfo>> cached = productCacheService.getProductList(sortType.name(), brandIdOrNull);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 캐시 미스: DB 조회
        List<Product> products;
        if (brandIdOrNull != null) {
            // 브랜드별 조회는 아직 정렬을 지원하지 않으므로 전체 조회 후 필터링
            products = productRepository.findAllSorted(sortType).stream()
                    .filter(p -> p.getBrandId().equals(brandIdOrNull))
                    .collect(Collectors.toList());
        } else {
            products = productRepository.findAllSorted(sortType);
        }

        // ProductDetail로 변환
        List<ProductDetailInfo> result = products.stream()
                .map(p -> productDetailService.getProductDetail(p.getId(), userIdOrNull))
                .map(ProductDetailInfo::from)
                .collect(Collectors.toList());

        // 캐시에 저장 (사용자별로 다르지 않은 목록 데이터만 캐시)
        productCacheService.cacheProductList(sortType.name(), brandIdOrNull, result);

        return result;
    }
}
