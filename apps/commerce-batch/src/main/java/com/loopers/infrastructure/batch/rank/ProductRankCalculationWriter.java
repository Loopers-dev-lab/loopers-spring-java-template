package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.rank.ProductRank;
import com.loopers.domain.rank.ProductRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductRank를 Materialized View에 저장하는 Writer.
 * <p>
 * Step 2 (랭킹 로직 실행 Step)에서 사용합니다.
 * 랭킹 번호가 부여된 ProductRank를 Materialized View에 저장합니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>Chunk 단위로 받은 ProductRank를 수집하고 저장</li>
 *   <li>각 Chunk마다 전체 ProductRank를 저장 (saveRanks가 delete + insert를 수행)</li>
 *   <li>기존 데이터 삭제 후 새 데이터 저장 (delete + insert 방식)</li>
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
public class ProductRankCalculationWriter implements ItemWriter<ProductRank> {

    private final ProductRankRepository productRankRepository;
    private final ProductRankAggregationProcessor productRankAggregationProcessor;
    private final List<ProductRank> allRanks = new java.util.ArrayList<>();

    /**
     * ProductRank Chunk를 수집하고 저장합니다.
     * <p>
     * 모든 Chunk를 메모리에 모아두고, 각 Chunk마다 전체를 저장합니다.
     * saveRanks가 delete + insert를 수행하므로, 각 Chunk마다 전체를 저장해도 문제없습니다.
     * </p>
     *
     * @param chunk 처리할 ProductRank Chunk
     * @throws Exception 처리 중 오류 발생 시
     */
    @Override
    public void write(Chunk<? extends ProductRank> chunk) throws Exception {
        List<? extends ProductRank> items = chunk.getItems()
            .stream()
            .filter(item -> item != null) // null 필터링 (TOP 100에 포함되지 않은 항목)
            .collect(Collectors.toList());

        if (items.isEmpty()) {
            return;
        }

        // 기간 정보 가져오기
        ProductRank.PeriodType periodType = productRankAggregationProcessor.getPeriodType();
        LocalDate periodStartDate = productRankAggregationProcessor.getPeriodStartDate();

        if (periodType == null || periodStartDate == null) {
            log.error("기간 정보가 설정되지 않았습니다. 건너뜁니다.");
            return;
        }

        // 모든 Chunk를 수집
        allRanks.addAll(items);
        log.debug("ProductRank Chunk 수집: count={}, total={}", items.size(), allRanks.size());

        // 각 Chunk마다 전체를 저장 (saveRanks가 delete + insert를 수행하므로 문제없음)
        log.info("ProductRank 저장: periodType={}, periodStartDate={}, total={}", 
            periodType, periodStartDate, allRanks.size());
        productRankRepository.saveRanks(periodType, periodStartDate, allRanks);
    }
}

