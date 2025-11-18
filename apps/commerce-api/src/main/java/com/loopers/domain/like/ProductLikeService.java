package com.loopers.domain.like;

import com.loopers.domain.product.ProductSortType;
import com.loopers.infrastructure.productLike.ProductLikeQueryRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductLikeService {
    private final ProductLikeRepository productLikeRepository;
    private final ProductLikeQueryRepository productLikeQueryRepository;

    @Transactional
    public int like(Long userPkId, Long productId) {
        return productLikeRepository.insertIgnore(userPkId, productId);
    }

    @Transactional
    public void dislike(Long userPkId, Long productId) {
        productLikeRepository.delete(userPkId, productId);
    }

    @Transactional(readOnly = true)
    public ProductLikeSummaryVO getProductLikeSummary(Long productId) {
        return productLikeQueryRepository.findProductDetail(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다"));
    }

    @Transactional(readOnly = true)
    public Page<ProductLikeSummaryVO> productLikeSummaryVOPage(ProductSortType sortType, Pageable pageable) {
        return productLikeQueryRepository.findProductLikes(sortType, pageable);
    }

}
