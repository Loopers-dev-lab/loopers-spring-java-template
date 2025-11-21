package com.loopers.domain.like.product;

import com.loopers.domain.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LikeProductService {
    private final LikeProductRepository likeProductRepository;

    public void likeProduct(Long userId, Long productId) {
        likeProductRepository.findByUserIdAndProductId(userId, productId)
                .ifPresentOrElse(BaseEntity::restore, () -> {
                    LikeProduct likeProduct = LikeProduct.create(userId, productId);
                    likeProductRepository.save(likeProduct);
                });
    }

    public void unlikeProduct(Long userId, Long productId) {
        likeProductRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(BaseEntity::delete);
    }

    public Page<LikeProduct> getLikedProducts(Long userId, Pageable pageable) {
        return likeProductRepository.getLikeProductsByUserIdAndDeletedAtIsNull(userId, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }
}
