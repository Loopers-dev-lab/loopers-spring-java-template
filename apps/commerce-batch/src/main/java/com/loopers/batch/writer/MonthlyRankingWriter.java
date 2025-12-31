package com.loopers.batch.writer;

import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.MonthlyRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

/**
 * ItemWriter for persisting monthly ranking data.
 *
 * <p>This writer saves MonthlyProductRank entities to the database.
 * It uses a delete-and-insert strategy to ensure data consistency
 * by removing existing data for the period before inserting new rankings.
 */
@Slf4j
@RequiredArgsConstructor
public class MonthlyRankingWriter implements ItemWriter<MonthlyProductRank> {

    private final MonthlyRankRepository repository;

    /**
     * Writes a chunk of monthly ranking data to the database.
     *
     * <p>Implementation strategy:
     * <ol>
     *   <li>Delete all existing rankings for the target month</li>
     *   <li>Insert new aggregated rankings</li>
     * </ol>
     *
     * @param chunk the chunk of rankings to write
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
