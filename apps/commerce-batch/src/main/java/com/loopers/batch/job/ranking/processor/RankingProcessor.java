package com.loopers.batch.job.ranking.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.loopers.batch.job.ranking.dto.RankingAggregation;

/**
 * 랭킹 데이터 처리기
 * - Reader에서 이미 점수 계산 및 순위 부여가 완료됨
 * - 추가 비즈니스 로직이 필요할 때 확장 포인트로 활용
 */
@Component
public class RankingProcessor implements ItemProcessor<RankingAggregation, RankingAggregation> {

    @Override
    public RankingAggregation process(RankingAggregation item) throws Exception {
        // Reader에서 이미 순위 부여됨
        // 추가 가공이 필요하면 여기서 처리
        // 예: 특정 조건 필터링, 데이터 보정 등

        // 현재는 단순 통과 (pass-through)
        return item;
    }
}
