package com.loopers.batch.job.productRankingJob.step.writer;
import com.loopers.domain.rank.MonthlyProductRank;
import com.loopers.domain.rank.MonthlyRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDate;

@RequiredArgsConstructor
public class MonthlyRankingWriter implements ItemWriter<MonthlyProductRank>, ItemStream {

	private final MonthlyRankRepository repository;
	private final LocalDate periodStart;
	private boolean initialized = false;

    @Override
	public void open(ExecutionContext executionContext) {
		if (!initialized) {
			repository.deleteByPeriodStart(periodStart);
			initialized = true;
		}
	}

	@Override
	public void update(ExecutionContext executionContext) { }

	@Override
	public void close() { }

	@Override
	public void write(Chunk<? extends MonthlyProductRank> chunk) {
		if (chunk.isEmpty()) {
			return;
		}
		repository.saveAll(chunk.getItems());
	}
}
