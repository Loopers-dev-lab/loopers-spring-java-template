package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 랭킹 점수 계산 서비스
 * 배치 작업을 통해 랭킹이 업데이트되므로, 실시간 Redis 업데이트는 하지 않음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final RankingWeightService rankingWeightService;

    /**
     * 주문 점수 계산
     * 동적 weight 적용
     */
    public double calculateOrderScore(double price, int quantity) {
        // 동적으로 weight 조회
        double orderWeight = rankingWeightService.getWeight(RankingEventType.ORDER);
        // log10(price * quantity + 1) * weight
        return Math.log10(price * quantity + 1) * orderWeight;
    }

    /**
     * 좋아요 점수 반환
     * 동적 weight 적용
     */
    public double getLikeScore() {
        return rankingWeightService.getWeight(RankingEventType.LIKE);
    }

    /**
     * 조회수 점수 반환
     * 동적 weight 적용
     */
    public double getViewScore() {
        return rankingWeightService.getWeight(RankingEventType.VIEW);
    }
}

