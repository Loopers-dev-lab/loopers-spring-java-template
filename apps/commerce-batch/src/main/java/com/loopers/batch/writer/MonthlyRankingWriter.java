package com.loopers.batch.writer;

import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.MonthlyRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월간 랭킹 데이터를 저장하는 ItemWriter.
 *
 * <p>이 Writer는 MonthlyProductRank 엔티티를 데이터베이스에 저장합니다.
 * 데이터 일관성을 보장하기 위해 삭제 후 삽입 전략을 사용하며,
 * 새로운 랭킹을 삽입하기 전에 해당 기간의 기존 데이터를 제거합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class MonthlyRankingWriter implements ItemWriter<MonthlyProductRank> {

    private final MonthlyRankRepository repository;

    /**
     * 월간 랭킹 데이터 청크를 데이터베이스에 저장합니다.
     *
     * <p>구현 전략:
     * <ol>
     *   <li>대상 월의 기존 랭킹 모두 삭제</li>
     *   <li>새로운 집계 랭킹 삽입</li>
     * </ol>
     *
     * @param chunk 저장할 랭킹 청크
     */
    @Override
    @Transactional
    public void write(Chunk<? extends MonthlyProductRank> chunk) {
        if (chunk.isEmpty()) {
            log.warn("Empty chunk received, skipping write operation");
            return;
        }

        String yearMonth = chunk.getItems().get(0).getYearMonth();
        log.info("Writing monthly rankings: yearMonth={}, count={}", yearMonth, chunk.size());

        repository.deleteByYearMonth(yearMonth);
        repository.saveAll(chunk.getItems());

        log.info("Successfully saved {} monthly rankings for month {}", chunk.size(), yearMonth);
    }
}
