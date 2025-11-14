package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductLikeService {
    private final ProductLikeRepository productLikeRepository;

    @Transactional
    public int like(Long userPkId, Long productId) {
        return productLikeRepository.insertIgnore(userPkId, productId);
    }

    @Transactional
    public void dislike(Long userPkId, Long productId) {
        productLikeRepository.delete(userPkId, productId);
    }

}
