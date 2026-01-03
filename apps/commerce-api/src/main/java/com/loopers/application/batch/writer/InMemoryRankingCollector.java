package com.loopers.application.batch.writer;

import com.loopers.application.batch.dto.RankedProductDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chunk 처리 중 RankedProduct를 메모리에 수집하는 Writer
 * - Step 완료 후 정렬하여 TOP 100을 선택하기 위함
 */
@Slf4j
public class InMemoryRankingCollector implements ItemWriter<RankedProductDto> {

    @Getter
    private final List<RankedProductDto> collectedItems = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void write(Chunk<? extends RankedProductDto> chunk) {
        collectedItems.addAll(chunk.getItems());
        log.debug("[Collector] Collected {} items, total: {}", chunk.size(), collectedItems.size());
    }

    /**
     * 수집된 데이터 초기화 (다음 실행을 위해)
     */
    public void clear() {
        collectedItems.clear();
    }

    /**
     * 수집된 데이터 개수
     */
    public int size() {
        return collectedItems.size();
    }
}
