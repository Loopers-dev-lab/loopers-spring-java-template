package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.rank.ProductRank;
import com.loopers.domain.rank.ProductRankScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * ProductRankScore를 ProductRank로 변환하는 Processor.
 * <p>
 * Step 2 (랭킹 로직 실행 Step)에서 사용합니다.
 * ProductRankScore를 읽어서 랭킹 번호를 부여하고 ProductRank로 변환합니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>ProductRankScore에 랭킹 번호 부여 (1부터 시작)</li>
 *   <li>TOP 100만 선정 (나머지는 null 반환하여 필터링)</li>
 *   <li>ProductRank로 변환</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ProductRankCalculationProcessor implements ItemProcessor<ProductRankScore, ProductRank> {

    private final ProductRankAggregationProcessor productRankAggregationProcessor;
    private int currentRank = 0;
    private static final int TOP_RANK_LIMIT = 100;

    /**
     * ProductRankScore를 ProductRank로 변환합니다.
     * <p>
     * 랭킹 번호를 부여하고, TOP 100에 포함되는 경우에만 ProductRank를 반환합니다.
     * </p>
     *
     * @param score ProductRankScore
     * @return ProductRank (TOP 100에 포함되는 경우), null (그 외)
     * @throws Exception 처리 중 오류 발생 시
     */
    @Override
    public ProductRank process(ProductRankScore score) throws Exception {
        int rank = ++currentRank;

        // TOP 100에 포함되지 않으면 null 반환 (필터링)
        if (rank > TOP_RANK_LIMIT) {
            return null;
        }

        // 기간 정보 가져오기
        ProductRank.PeriodType periodType = productRankAggregationProcessor.getPeriodType();
        LocalDate periodStartDate = productRankAggregationProcessor.getPeriodStartDate();

        if (periodType == null || periodStartDate == null) {
            log.error("기간 정보가 설정되지 않았습니다. 건너뜁니다.");
            return null;
        }

        // ProductRank 생성 (랭킹 번호 부여)
        ProductRank productRank = new ProductRank(
            periodType,
            periodStartDate,
            score.getProductId(),
            rank, // 랭킹 번호 (1부터 시작)
            score.getLikeCount(),
            score.getSalesCount(),
            score.getViewCount()
        );

        return productRank;
    }
}

