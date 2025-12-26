package com.loopers.application.ranking;

import com.loopers.config.ranking.RankingProperties;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingWeightPolicy;
import com.loopers.domain.ranking.RankingWeightPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 랭킹 Weight 관리 서비스
 * DB의 Weight Policy를 조회하여 동적으로 weight를 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingWeightService {

    private final RankingWeightPolicyRepository weightPolicyRepository;
    private final RankingProperties rankingProperties; // fallback용

    // 캐시 (성능 최적화)
    private final Map<RankingEventType, Double> weightCache = new ConcurrentHashMap<>();

    /**
     * 특정 이벤트 타입의 weight 조회
     * DB에 없으면 기본값(fallback) 사용
     */
    @Transactional(readOnly = true)
    public double getWeight(RankingEventType eventType) {
        // 캐시 확인
        Double cachedWeight = weightCache.get(eventType);
        if (cachedWeight != null) {
            return cachedWeight;
        }

        // DB 조회
        Double weight = weightPolicyRepository.findByEventType(eventType)
                .filter(RankingWeightPolicy::getIsActive)
                .map(RankingWeightPolicy::getWeight)
                .orElseGet(() -> getDefaultWeight(eventType));

        // 캐시에 저장
        weightCache.put(eventType, weight);
        return weight;
    }

    /**
     * 모든 weight 캐시 갱신
     * Weight Policy 변경 시 호출
     */
    @Transactional(readOnly = true)
    public void refreshCache() {
        log.info("Refreshing ranking weight cache...");
        weightCache.clear();
        
        Map<RankingEventType, Double> activeWeights = weightPolicyRepository.findAllActive()
                .stream()
                .collect(Collectors.toMap(
                        RankingWeightPolicy::getEventType,
                        RankingWeightPolicy::getWeight
                ));

        weightCache.putAll(activeWeights);
        log.info("Weight cache refreshed: {}", activeWeights);
    }

    /**
     * 기본 weight 반환 (fallback)
     */
    private double getDefaultWeight(RankingEventType eventType) {
        RankingProperties.Weights weights = rankingProperties.weights();
        return switch (eventType) {
            case ORDER -> weights.order();
            case LIKE -> weights.like();
            case VIEW -> weights.view();
        };
    }
}

