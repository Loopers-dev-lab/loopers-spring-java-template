package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductLikeCountService {

    private final ProductLikeCountRepository productLikeCountRepository;

    @Transactional
    public void reflectLike(Long productId) { productLikeCountRepository.upsertLikeCount(productId); }

    @Transactional
    public void reflectUnlike(Long productId) {
        productLikeCountRepository.decreaseLikeCount(productId);
    }

    public Optional<ProductLikeCount> findById(Long productId) {
        return productLikeCountRepository.findById(productId);
    }

    @Transactional
    public ProductLikeCount save(ProductLikeCount productLikeCount) {
        return productLikeCountRepository.save(productLikeCount);
    }

    public Page<ProductLikeCount> getProductLikeCountsOrderByLikeCount(Long brandId, Pageable pageable) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ProductSortType.toSort("likes_desc")
        );
        return productLikeCountRepository.findPageOrderByLikeCountDesc(brandId, pageRequest);
    }
}
