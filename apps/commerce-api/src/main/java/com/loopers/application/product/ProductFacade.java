package com.loopers.application.product;

import com.loopers.application.event.product.ProductViewedEvent;
import com.loopers.domain.like.service.LikeReadService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.service.ProductReadService;
import com.loopers.domain.product.command.ProductSearchFilter;
import com.loopers.domain.product.enums.ProductSortCondition;
import com.loopers.infrastructure.cache.ProductDetailCache;
import com.loopers.infrastructure.cache.ProductListCache;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
@Transactional
public class ProductFacade {

    private final ProductReadService productReadService;
    private final LikeReadService likeReadService;
    private final ProductDetailCache productDetailCache;
    private final ProductListCache productListCache;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<ProductSummaryInfo> getProducts(ProductSearchCommand command) {
        // Cache only for LIKES_DESC sort
        if (command.getSort() == ProductSortCondition.LIKES_DESC) {
            return productListCache.get(
                    command.getBrandId(),
                    command.getSort(),
                    command.getPage(),
                    command.getSize()
            ).orElseGet(() -> {
                Page<ProductSummaryInfo> result = fetchProducts(command);
                productListCache.set(
                        command.getBrandId(),
                        command.getSort(),
                        command.getPage(),
                        command.getSize(),
                        result
                );
                return result;
            });
        }

        return fetchProducts(command);
    }

    private Page<ProductSummaryInfo> fetchProducts(ProductSearchCommand command) {
        ProductSearchFilter filter = ProductSearchFilter.of(
                command.getBrandId(),
                command.getKeyword(),
                command.getSort()
        );

        Pageable pageable = PageRequest.of(command.getPage(), command.getSize());

        return productReadService.getProducts(
                filter,
                pageable,
                command.getMemberIdOrNull()
        );
    }

    @Transactional(readOnly = true)
    public CursorPageInfo<ProductSummaryInfo> getProductsByCursor(ProductCursorSearchCommand command) {
        ProductSearchFilter filter = ProductSearchFilter.of(
                command.getBrandId(),
                command.getKeyword(),
                command.getSort()
        );

        return productReadService.getProductsByCursor(
                filter,
                command.getCursor(),
                command.getSize(),
                command.getMemberIdOrNull()
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, Long memberIdOrNull) {
        // 1. 캐시에서 상품 정보 조회 (isLikedByMember=false인 상태)
        ProductDetailInfo cachedInfo = productDetailCache.get(productId)
                .orElseGet(() -> {
                    // 캐시 miss: DB 조회 (isLikedByMember는 나중에 계산)
                    ProductDetailInfo result = productReadService.getProductDetail(productId, null);
                    productDetailCache.set(productId, result);
                    return result;
                });

        // 2. Product 엔티티 조회 (brandId 획득용)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.loopers.support.error.CoreException(
                        com.loopers.support.error.ErrorType.NOT_FOUND,
                        "상품을 찾을 수 없습니다."));

        // 3. isLikedByMember 동적 계산
        boolean isLiked = memberIdOrNull != null && likeReadService.isLikedBy(memberIdOrNull, productId);

        // 4. isLikedByMember 필드만 교체해서 반환
        ProductDetailInfo result = ProductDetailInfo.builder()
                .id(cachedInfo.getId())
                .name(cachedInfo.getName())
                .description(cachedInfo.getDescription())
                .brandName(cachedInfo.getBrandName())
                .brandDescription(cachedInfo.getBrandDescription())
                .price(cachedInfo.getPrice())
                .stock(cachedInfo.getStock())
                .likeCount(cachedInfo.getLikeCount())
                .isLikedByMember(isLiked)  // ⭐ 동적 계산
                .build();

        // 5. ProductViewedEvent 발행 (조회수 집계)
        eventPublisher.publishEvent(new ProductViewedEvent(
            memberIdOrNull,  // 비로그인 사용자는 null
            productId,
            product.getBrandId(),
            LocalDateTime.now()
        ));

        return result;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryInfo> getPopularProducts(Long memberIdOrNull) {
        // 캐시 제거 - 순수 DB 조회
        return productReadService.getPopularProducts(memberIdOrNull);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryInfo> getBrandPopularProducts(Long brandId, int limit, Long memberIdOrNull) {
        // 캐시 제거 - 순수 DB 조회
        return productReadService.getBrandPopularProducts(brandId, limit, memberIdOrNull);
    }
}