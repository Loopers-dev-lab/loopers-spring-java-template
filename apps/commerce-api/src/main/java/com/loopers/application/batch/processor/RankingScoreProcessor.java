package com.loopers.application.batch.processor;

import com.loopers.application.batch.dto.ProductMetricsDto;
import com.loopers.application.batch.dto.RankedProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * ProductMetrics를 읽어서 점수를 계산하는 Processor
 */
@Slf4j
public class RankingScoreProcessor implements ItemProcessor<ProductMetricsDto, RankedProductDto> {

    private final double viewWeight;
    private final double likeWeight;
    private final double orderWeight;

    /**
     * 생성자 주입으로 weight 값 받기
     */
    public RankingScoreProcessor(double viewWeight, double likeWeight, double orderWeight) {
        this.viewWeight = viewWeight;
        this.likeWeight = likeWeight;
        this.orderWeight = orderWeight;
    }

    @Override
    public RankedProductDto process(ProductMetricsDto item) {
        double totalScore = calculateTotalScore(
            item.viewCount(),
            item.likeCount(),
            item.salesAmount()
        );

        return new RankedProductDto(
            item.productId(),
            totalScore,
            item.likeCount(),
            item.viewCount(),
            item.salesCount(),
            item.salesAmount()
        );
    }

    /**
     * 총 점수 계산 (기존 Redis 방식과 동일한 가중치)
     */
    private double calculateTotalScore(long viewCount, long likeCount, long salesAmount) {
        double viewScore = viewCount * viewWeight;
        double likeScore = likeCount * likeWeight;

        // 주문 점수: log 정규화 적용 (고가 상품 편중 방지)
        double orderScore = orderWeight * Math.log1p(salesAmount);

        return viewScore + likeScore + orderScore;
    }
}
