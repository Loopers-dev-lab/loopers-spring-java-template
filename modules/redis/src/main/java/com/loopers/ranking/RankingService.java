package com.loopers.ranking;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingService {

    private final RankingRedisRepository rankingRedisRepository;
    private final RankingKeyGenerator rankingKeyGenerator;
    private final RankingScorePolicy rankingScorePolicy;

    public void recordLikeEvent(Long productId, long delta, ZonedDateTime occurredAt) {
        double score = rankingScorePolicy.likeScore(delta);
        increaseScore(productId, score, occurredAt);
    }

    public void recordViewEvent(Long productId, ZonedDateTime occurredAt) {
        double score = rankingScorePolicy.viewScore();
        increaseScore(productId, score, occurredAt);
    }

    public void recordOrderEvent(Long productId, Long price, Long quantity, ZonedDateTime occurredAt) {
        double score = rankingScorePolicy.orderScore(price, quantity);
        increaseScore(productId, score, occurredAt);
    }

    private void increaseScore(Long productId, double score, ZonedDateTime occurredAt) {
        if (productId == null || score == 0d) {
            return;
        }
        String key = rankingKeyGenerator.resolve(occurredAt);
        rankingRedisRepository.increaseScore(key, productId.toString(), score, rankingKeyGenerator.ttl());
    }

    public List<RankingRow> getRankingRows(String date, int page, int size) {
        String key = rankingKeyGenerator.resolve(date);
        long start = (long) page * size;
        long end = start + size - 1;
        return rankingRedisRepository.fetchPage(key, start, end);
    }

    public long count(String date) {
        return rankingRedisRepository.count(rankingKeyGenerator.resolve(date));
    }

    public Long findRank(Long productId, String date) {
        String key = rankingKeyGenerator.resolve(date);
        return rankingRedisRepository.findRank(key, productId);
    }

    public void carryOver(LocalDate date, double weight) {
        LocalDate target = date != null ? date : LocalDate.now(ZoneId.of("Asia/Seoul"));
        String sourceKey = rankingKeyGenerator.resolve(target.minusDays(1));
        String targetKey = rankingKeyGenerator.resolve(target);
        rankingRedisRepository.scaleFrom(sourceKey, targetKey, weight, rankingKeyGenerator.ttl());
    }
}
