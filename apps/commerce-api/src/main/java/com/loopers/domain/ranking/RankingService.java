package com.loopers.domain.ranking;

import com.loopers.infrastructure.cache.RankingCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class RankingService {

    private final RankingCacheService rankingCacheService;

    public List<Ranking> getRanking(LocalDate date, int page, int size) {
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        List<RankingItem> rankingItems = rankingCacheService.getRankingRange(date, start, end);

        if (rankingItems.isEmpty()) {
            return new ArrayList<>();
        }

        return IntStream.range(0, rankingItems.size())
                .mapToObj(i -> {
                    RankingItem item = rankingItems.get(i);
                    long rank = start + 1 + i;
                    return new Ranking(item.productId(), rank, item.score());
                })
                .toList();
    }
}

