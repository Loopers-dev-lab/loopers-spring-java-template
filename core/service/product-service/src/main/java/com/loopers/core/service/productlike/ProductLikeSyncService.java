package com.loopers.core.service.productlike;

import com.loopers.core.domain.product.repository.ProductLikeCacheRepository;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.service.productlike.component.ProductLikeCountSynchronizer;
import com.loopers.core.service.productlike.component.ProductLikeSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLikeSyncService {

    private final ProductLikeCacheRepository productLikeCacheRepository;
    private final ProductLikeSynchronizer productLikeSynchronizer;
    private final ProductLikeCountSynchronizer productLikeCountSynchronizer;

    @Transactional
    public void sync() {
        long currentTime = System.currentTimeMillis();
        long lastSyncedTime = productLikeCacheRepository.getLastSyncTime();

        List<ProductLikeCache> newCachedLikes = productLikeCacheRepository.getLikesSinceLastSync(lastSyncedTime, currentTime);
        List<ProductLikeCache> newCachedUnlikes = productLikeCacheRepository.getUnlikesSinceLastSync(lastSyncedTime, currentTime);

        // DB에 좋아요 생성 및 삭제
        productLikeSynchronizer.sync(newCachedLikes, newCachedUnlikes);

        // 상품의 좋아요 카운트 업데이트
        productLikeCountSynchronizer.sync(newCachedLikes, newCachedUnlikes);

        // 다음 동기화의 시작점으로 설정 (currentTime 이후의 데이터는 다음 배치에서 처리)
        productLikeCacheRepository.updateLastSyncTime(currentTime);
    }
}
