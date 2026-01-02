package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * packageName : com.loopers.domain.product
 * fileName     : ProductDetailService
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class ProductDomainService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final LikeRepository likeRepository;

    @Transactional(readOnly = true)
    public ProductDetail getProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다"));
        long likeCount = likeRepository.countByProductId(product.getId());

        return ProductDetail.of(product, brand, likeCount);
    }
}
