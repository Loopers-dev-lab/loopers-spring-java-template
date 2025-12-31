package com.loopers.batch.writer;

import com.loopers.domain.rank.WeeklyProductRank;
import com.loopers.domain.rank.WeeklyRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

/**
 * ItemWriter for persisting weekly ranking data.
 *
 * <p>This writer saves WeeklyProductRank entities to the database.
 * It uses a delete-and-insert strategy to ensure data consistency
 * by removing existing data for the period before inserting new rankings.
 */
@Slf4j
@RequiredArgsConstructor
public class WeeklyRankingWriter implements ItemWriter<WeeklyProductRank> {

    private final WeeklyRankRepository repository;

    /**
     * Writes a chunk of weekly ranking data to the database.
     *
     * <p>Implementation strategy:
     * <ol>
     *   <li>Delete all existing rankings for the target week</li>
     *   <li>Insert new aggregated rankings</li>
     * </ol>
     *
     * @param chunk the chunk of rankings to write
     */
    @Override
    @Transactional
    public void write(Chunk<? extends WeeklyProductRank> chunk) {
        if (chunk.isEmpty()) {
            log.warn("Empty chunk received, skipping write operation");
            return;
        }

        String yearWeek = chunk.getItems().get(0).getYearWeek();
        log.info("Writing weekly rankings: yearWeek={}, count={}", yearWeek, chunk.size());

        repository.deleteByYearWeek(yearWeek);
        repository.saveAll(chunk.getItems());

        log.info("Successfully saved {} weekly rankings for week {}", chunk.size(), yearWeek);
    }
}
