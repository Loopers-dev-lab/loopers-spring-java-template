package com.loopers.domain.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.like.LikeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDomainService {

    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;

    /**
     * 상품 상세 조회 (Product + Brand + 좋아요 수 조합)
     */
    public ProductInfo getProductWithDetails(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Long likeCount = likeRepository.countByProductId(productId);

        return ProductInfo.from(product, likeCount);
    }

    /**
     * 상품 목록 조회 (Product + Brand + 좋아요 수 조합)
     */
    public Page<ProductInfo> getProductsWithDetails(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return enrichWithLikeCounts(products);
    }

    /**
     * 정렬 조건을 고려한 상품 목록 조회
     */
    public Page<ProductInfo> getProductsWithDetails(Pageable pageable, ProductSortType sortType) {
        Page<Product> products = productRepository.findAllSorted(pageable, sortType);
        return enrichWithLikeCounts(products);
    }

    /**
     * 브랜드별 상품 목록 조회 (정렬 조건 포함)
     */
    public Page<ProductInfo> getProductsByBrandWithDetails(Long brandId, Pageable pageable, ProductSortType sortType) {
        Page<Product> products = productRepository.findByBrandIdSorted(brandId, pageable, sortType);
        return enrichWithLikeCounts(products);
    }

    /**
     * 상품 목록에 좋아요 수를 일괄 조회하여 추가 (N+1 쿼리 방지)
     */
    private Page<ProductInfo> enrichWithLikeCounts(Page<Product> products) {
        if (products.isEmpty()) {
            return Page.empty(products.getPageable());
        }

        List<Long> productIds = products.getContent().stream()
            .map(Product::getId)
            .collect(Collectors.toList());

        Map<Long, Long> likeCountMap = likeRepository.countByProductIds(productIds);

        List<ProductInfo> productInfos = products.getContent().stream()
            .map(product -> ProductInfo.from(product, likeCountMap.getOrDefault(product.getId(), 0L)))
            .collect(Collectors.toList());

        return new PageImpl<>(productInfos, products.getPageable(), products.getTotalElements());
    }
}
