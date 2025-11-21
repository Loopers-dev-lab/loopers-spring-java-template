package com.loopers.domain.like.service;

import com.loopers.domain.like.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class LikeReadService {

    private final LikeRepository likeRepository;

    public long countByProductId(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    public boolean isLikedBy(String memberId, Long productId) {
        if (memberId == null) {
            return false;
        }
        return likeRepository.existsByMemberIdAndProductId(memberId, productId);
    }

    public Set<Long> findLikedProductIds(String memberId, List<Long> productIds) {
        return likeRepository.findLikedProductIds(memberId, productIds);
    }
}