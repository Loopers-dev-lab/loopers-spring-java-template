package com.loopers.core.service.productlike.component;


import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductLikeSynchronizer {

    private final ProductLikeRepository repository;

    public void sync(List<ProductLikeCache> cachedLike, List<ProductLikeCache> cachedUnlike) {
        List<ProductLike> likes = cachedLike.stream()
                .map(cache -> ProductLike.create(
                        cache.userId(),
                        cache.productId(),
                        new CreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(cache.timestamp()), ZoneId.systemDefault()))
                ))
                .toList();
        repository.bulkSaveOrUpdate(likes);
        repository.bulkDelete(cachedUnlike);
    }
}
