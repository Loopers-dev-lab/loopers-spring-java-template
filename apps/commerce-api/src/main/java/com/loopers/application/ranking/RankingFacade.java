package com.loopers.application.ranking;

import com.loopers.interfaces.api.ranking.RankingV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RankingFacade {
    private final StringRedisTemplate redisTemplate;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    public RankingV1Dto.ProductRankingPageResponse getDailyProductRanking(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        String date = LocalDate.now(KST).format(YYYYMMDD);

        String key = "ranking:all:" + date;
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        Long total = zset.size(key);
        long totalElements = (total == null) ? 0 : total;

        if (totalElements == 0) {
            return new RankingV1Dto.ProductRankingPageResponse(date, page, size, 0, 0, List.of());
        }

        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        if (start >= totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            return new RankingV1Dto.ProductRankingPageResponse(date, page, size, totalElements, totalPages, List.of());
        }

        Set<ZSetOperations.TypedTuple<String>> tuples =
                zset.reverseRangeWithScores(key, start, end);

        List<RankingV1Dto.ProductRankingResponse> items = new ArrayList<>();
        if (tuples != null) {
            long rank = start + 1;
            for (var t : tuples) {
                String member = t.getValue();
                Double score = t.getScore();
                if (member == null || score == null) continue;

                items.add(new RankingV1Dto.ProductRankingResponse(
                        rank++,
                        Long.parseLong(member),
                        score
                ));
            }
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new RankingV1Dto.ProductRankingPageResponse(date, page, size, totalElements, totalPages, items);
    }

}
