package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductSortType;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.cache.ProductDetailCache;
import com.loopers.interfaces.api.product.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFacade {

    private final ProductDomainService productDomainService;
    private final BrandDomainService brandDomainService;
    private final ProductCacheService productCacheService;

    /**
     * 상품 목록 조회 (Cache-Aside 패턴)
     * <p>
     * 1. 캐시 조회
     * 2. Cache Hit: 캐시된 상품 목록 직접 반환
     * 3. Cache Miss: DB 조회 → 캐시 저장
     */
    public ProductDto.ProductListResponse getProducts(
            Long brandId,
            String sort,
            int page,
            int size
    ) {
        Optional<ProductDto.ProductListResponse> cachedResponse =
                productCacheService.getProductListResponse(brandId, sort, page, size);

        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        if (brandId != null) {
            brandDomainService.getActiveBrand(brandId);
        }

        ProductSortType sortType = ProductSortType.from(sort);
        Page<Product> products = productDomainService.getProducts(brandId, sortType, page, size);

        Set<Long> brandIds = products.getContent().stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet());
        Map<Long, Brand> brandMap = brandDomainService.getBrandMap(brandIds);

        ProductDto.ProductListResponse response = ProductDto.ProductListResponse.from(products, brandMap);

        productCacheService.setProductListResponse(brandId, sort, page, size, response);

        return response;
    }

    /**
     * 상품 상세 조회 (Cache-Aside 패턴)
     * <p>
     * 1. 캐시 조회
     * 2. Cache Hit: 캐시된 데이터 직접 반환 (id만 URL에서)
     * 3. Cache Miss: DB 조회 → 캐시 저장 → 반환
     */
    public ProductDto.ProductDetailResponse getProduct(Long productId) {
        // 1. 캐시 조회 시도
        Optional<ProductDetailCache> cachedDetail = productCacheService.getProductDetail(productId);

        if (cachedDetail.isPresent()) {
            // Cache Hit: 캐시된 데이터 직접 반환
            return ProductDto.ProductDetailResponse.from(
                    productId,
                    cachedDetail.get()
            );
        }

        // Cache Miss: DB에서 조회
        Product product = productDomainService.getProduct(productId);
        Brand brand = brandDomainService.getBrand(product.getBrandId());

        // 캐시 데이터 생성 및 저장
        ProductDetailCache cache = ProductDetailCache.from(product, brand);
        productCacheService.setProductDetail(productId, cache);

        // Response 반환
        return ProductDto.ProductDetailResponse.from(productId, cache);
    }
}
